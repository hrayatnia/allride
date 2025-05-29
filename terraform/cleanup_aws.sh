#!/bin/bash

set -e

REGION="us-east-1"
PROJECT="allride"

echo "Starting cleanup of AWS resources for $PROJECT in $REGION..."

# Function to wait for deletion
wait_for_deletion() {
    local resource_type=$1
    local resource_id=$2
    local check_command=$3
    
    echo "Waiting for $resource_type $resource_id to be deleted..."
    while $check_command; do
        echo "Still waiting for deletion..."
        sleep 10
    done
    echo "$resource_type $resource_id has been deleted."
}

# Delete MSK Clusters
echo "Cleaning up MSK clusters..."
MSK_CLUSTERS=$(aws kafka list-clusters --region $REGION --query "ClusterInfoList[?contains(ClusterName, '$PROJECT')].[ClusterArn]" --output text || true)
for CLUSTER_ARN in $MSK_CLUSTERS; do
    if [ ! -z "$CLUSTER_ARN" ]; then
        echo "Checking status of MSK cluster: $CLUSTER_ARN"
        CLUSTER_STATE=$(aws kafka describe-cluster --cluster-arn $CLUSTER_ARN --region $REGION --query 'ClusterInfo.State' --output text || echo "UNKNOWN")
        if [ "$CLUSTER_STATE" != "CREATING" ]; then
            echo "Deleting MSK cluster: $CLUSTER_ARN"
            aws kafka delete-cluster --cluster-arn $CLUSTER_ARN --region $REGION || true
        else
            echo "Cluster $CLUSTER_ARN is in CREATING state, skipping..."
        fi
    fi
done

# Delete ElastiCache Clusters
echo "Cleaning up ElastiCache clusters..."
CACHE_CLUSTERS=$(aws elasticache describe-cache-clusters --region $REGION --query "CacheClusters[?contains(CacheClusterId, '$PROJECT')].[CacheClusterId]" --output text || true)
for CLUSTER_ID in $CACHE_CLUSTERS; do
    if [ ! -z "$CLUSTER_ID" ]; then
        echo "Deleting ElastiCache cluster: $CLUSTER_ID"
        aws elasticache delete-cache-cluster --cache-cluster-id $CLUSTER_ID --region $REGION || true
    fi
done

# Wait for ElastiCache clusters to be deleted
echo "Waiting for ElastiCache clusters to be deleted..."
sleep 60

# Delete ElastiCache Subnet Groups
echo "Cleaning up ElastiCache subnet groups..."
CACHE_SUBNET_GROUPS=$(aws elasticache describe-cache-subnet-groups --region $REGION --query "CacheSubnetGroups[?contains(CacheSubnetGroupName, '$PROJECT')].[CacheSubnetGroupName]" --output text || true)
for SUBNET_GROUP in $CACHE_SUBNET_GROUPS; do
    if [ ! -z "$SUBNET_GROUP" ]; then
        echo "Deleting ElastiCache subnet group: $SUBNET_GROUP"
        aws elasticache delete-cache-subnet-group --cache-subnet-group-name $SUBNET_GROUP --region $REGION || true
    fi
done

# Delete RDS Instances
echo "Cleaning up RDS instances..."
RDS_INSTANCES=$(aws rds describe-db-instances --region $REGION --query "DBInstances[?contains(DBInstanceIdentifier, '$PROJECT')].[DBInstanceIdentifier]" --output text || true)
for DB_INSTANCE in $RDS_INSTANCES; do
    if [ ! -z "$DB_INSTANCE" ]; then
        echo "Deleting RDS instance: $DB_INSTANCE"
        aws rds delete-db-instance --db-instance-identifier $DB_INSTANCE --skip-final-snapshot --region $REGION || true
    fi
done

# Wait for RDS instances to be deleted
echo "Waiting for RDS instances to be deleted..."
sleep 60

# Delete RDS Subnet Groups
echo "Cleaning up RDS subnet groups..."
DB_SUBNET_GROUPS=$(aws rds describe-db-subnet-groups --region $REGION --query "DBSubnetGroups[?contains(DBSubnetGroupName, '$PROJECT')].[DBSubnetGroupName]" --output text || true)
for SUBNET_GROUP in $DB_SUBNET_GROUPS; do
    if [ ! -z "$SUBNET_GROUP" ]; then
        echo "Deleting RDS subnet group: $SUBNET_GROUP"
        aws rds delete-db-subnet-group --db-subnet-group-name $SUBNET_GROUP --region $REGION || true
    fi
done

# Delete VPC Resources
echo "Cleaning up VPC resources..."
VPC_IDS=$(aws ec2 describe-vpcs --region $REGION --filters "Name=tag:Name,Values=*$PROJECT*" --query 'Vpcs[*].VpcId' --output text || true)
for VPC_ID in $VPC_IDS; do
    if [ ! -z "$VPC_ID" ]; then
        echo "Processing VPC: $VPC_ID"
        
        # Delete NAT Gateways
        NAT_GATEWAYS=$(aws ec2 describe-nat-gateways --region $REGION --filter "Name=vpc-id,Values=$VPC_ID" --query 'NatGateways[*].NatGatewayId' --output text || true)
        for NAT_GW in $NAT_GATEWAYS; do
            echo "Deleting NAT Gateway: $NAT_GW"
            aws ec2 delete-nat-gateway --nat-gateway-id $NAT_GW --region $REGION || true
        done

        # Wait for NAT Gateways to be deleted
        echo "Waiting for NAT Gateways to be deleted..."
        sleep 30

        # Delete Route Tables (except main)
        ROUTE_TABLES=$(aws ec2 describe-route-tables --region $REGION --filters "Name=vpc-id,Values=$VPC_ID" --query 'RouteTables[?Associations[0].Main!=`true`].RouteTableId' --output text || true)
        for RT in $ROUTE_TABLES; do
            echo "Deleting route table: $RT"
            aws ec2 delete-route-table --route-table-id $RT --region $REGION || true
        done

        # Delete Internet Gateway
        IGW_ID=$(aws ec2 describe-internet-gateways --region $REGION --filters "Name=attachment.vpc-id,Values=$VPC_ID" --query 'InternetGateways[*].InternetGatewayId' --output text || true)
        if [ ! -z "$IGW_ID" ]; then
            echo "Detaching and deleting Internet Gateway: $IGW_ID"
            aws ec2 detach-internet-gateway --internet-gateway-id $IGW_ID --vpc-id $VPC_ID --region $REGION || true
            aws ec2 delete-internet-gateway --internet-gateway-id $IGW_ID --region $REGION || true
        fi

        # Delete Security Groups (except default)
        SEC_GROUPS=$(aws ec2 describe-security-groups --region $REGION --filters "Name=vpc-id,Values=$VPC_ID" --query 'SecurityGroups[?GroupName!=`default`].GroupId' --output text || true)
        for SG in $SEC_GROUPS; do
            echo "Deleting security group: $SG"
            aws ec2 delete-security-group --group-id $SG --region $REGION || true
        done

        # Delete Subnets
        SUBNETS=$(aws ec2 describe-subnets --region $REGION --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text || true)
        for SUBNET in $SUBNETS; do
            echo "Deleting subnet: $SUBNET"
            aws ec2 delete-subnet --subnet-id $SUBNET --region $REGION || true
        done

        # Delete VPC
        echo "Deleting VPC: $VPC_ID"
        aws ec2 delete-vpc --vpc-id $VPC_ID --region $REGION || true
    fi
done

# Delete Elastic IPs
echo "Cleaning up Elastic IPs..."
EIPS=$(aws ec2 describe-addresses --region $REGION --query 'Addresses[?contains(Tags[?Key==`Name`].Value, `'$PROJECT'`)].[AllocationId]' --output text || true)
for EIP in $EIPS; do
    if [ ! -z "$EIP" ]; then
        echo "Releasing Elastic IP: $EIP"
        aws ec2 release-address --allocation-id $EIP --region $REGION || true
    fi
done

echo "Cleanup completed!" 