output "heroku_app_url" {
  description = "URL of the Heroku application"
  value       = "https://${heroku_app.allride.name}.herokuapp.com"
}

output "sqs_queue_url" {
  description = "URL of the main SQS queue"
  value       = aws_sqs_queue.main_queue.url
  sensitive   = true
}

output "sqs_dlq_url" {
  description = "URL of the dead letter queue"
  value       = aws_sqs_queue.dlq.url
  sensitive   = true
}

output "postgres_endpoint" {
  description = "PostgreSQL endpoint"
  value       = aws_db_instance.postgres.endpoint
  sensitive   = true
}

output "memcached_endpoint" {
  description = "Memcached endpoint"
  value       = aws_elasticache_cluster.memcached.configuration_endpoint
  sensitive   = true
}

output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnets" {
  description = "IDs of public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnets" {
  description = "IDs of private subnets"
  value       = aws_subnet.private[*].id
} 