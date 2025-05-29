resource "aws_db_subnet_group" "postgres" {
  name        = "${var.app_name}-postgres-subnet-group"
  description = "Subnet group for PostgreSQL RDS"
  subnet_ids  = aws_subnet.private[*].id

  tags = {
    Name        = "${var.app_name}-postgres-subnet-group"
    Environment = var.environment
  }
}

resource "aws_elasticache_subnet_group" "memcached" {
  name        = "${var.app_name}-memcached-subnet-group"
  description = "Subnet group for Memcached ElastiCache"
  subnet_ids  = aws_subnet.private[*].id

  tags = {
    Name        = "${var.app_name}-memcached-subnet-group"
    Environment = var.environment
  }
} 