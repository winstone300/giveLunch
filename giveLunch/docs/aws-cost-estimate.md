# GiveLunch AWS Cost Estimate

## Baseline
- Region: `ap-northeast-2`
- Goal: portfolio-grade single-instance deployment

## Expected Services
- EC2: app host
- EBS: root volume for EC2
- RDS MySQL: primary database
- ElastiCache Redis: ranking/cache
- CloudWatch: logs and host metrics
- Data transfer: public traffic and package upload

## Cost Notes
- RDS and ElastiCache usually dominate cost for small projects.
- Single-AZ is acceptable for this branch because high availability is out of scope.
- Stop or resize unused non-production instances.
- Keep log retention limited in CloudWatch to avoid hidden growth.

## Cost Review Points
- Check if ElastiCache is necessary for the always-on branch environment.
- Keep EC2 instance type small until load testing justifies change.
- Review RDS storage autoscaling and backup retention.
- Remove idle public IPs, snapshots, and old release files.
