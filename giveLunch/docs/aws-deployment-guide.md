# GiveLunch AWS Deployment Guide

## Target Architecture
- Region: `ap-northeast-2`
- App: EC2 single instance + `systemd` + Nginx
- Database: RDS MySQL
- Cache: ElastiCache Redis
- Logs and host metrics: CloudWatch Agent
- CI/CD: GitHub Actions on `aws_deploy`

## Week 1: Application Configuration
1. Use `application.yml` for shared settings only.
2. Use `application-local.yml` for local development.
3. Use `application-prod.yml` for AWS runtime values injected from `/etc/givelunch/givelunch.env`.
4. Keep production secrets out of Git. Copy `deploy/env/givelunch.prod.env.example` and fill the real values on EC2.

## Week 2: AWS Infrastructure
1. Create one EC2 instance in a public subnet.
2. Create RDS MySQL and ElastiCache Redis in private subnets.
3. Security groups:
   - EC2 inbound: `80`, `22`
   - RDS inbound: `3306` from EC2 SG only
   - Redis inbound: `6379` from EC2 SG only
4. Attach an IAM role with CloudWatch and SSM permissions to EC2.

## Week 3: First Manual Deployment
1. Build the jar in [pom.xml](/D:/portPolio/giveLunch/giveLunch/pom.xml).
2. Upload the jar to `/opt/givelunch/releases/<timestamp>/`.
3. Install the templates from:
   - [givelunch.service](/D:/portPolio/giveLunch/giveLunch/deploy/systemd/givelunch.service)
   - [givelunch.conf](/D:/portPolio/giveLunch/giveLunch/deploy/nginx/givelunch.conf)
   - [amazon-cloudwatch-agent.json](/D:/portPolio/giveLunch/giveLunch/deploy/cloudwatch/amazon-cloudwatch-agent.json)
4. Run [deploy.sh](/D:/portPolio/giveLunch/giveLunch/deploy/scripts/deploy.sh) with the built jar.
5. Validate:
   - `http://<ec2-public-ip>/actuator/health`
   - `http://<ec2-public-ip>/login`
   - `http://<ec2-public-ip>/roulette`

## Week 4: GitHub Actions
1. Add repository secrets:
   - `EC2_HOST`
   - `EC2_USER`
   - `EC2_SSH_KEY`
2. Push to `aws_deploy` to trigger [aws_deploy.yml](/D:/portPolio/giveLunch/.github/workflows/aws_deploy.yml).
3. Confirm that the workflow builds, uploads the jar, restarts the service, and runs the health check.
4. If health checks fail, `rollback.sh` resets `current` to the previous release.

## Week 5: Operations
1. Ship Nginx and system logs to CloudWatch.
2. Monitor:
   - JVM memory and restarts
   - RDS connection and CPU
   - Redis latency and memory
   - `/actuator/health`
3. Keep a merge checklist before moving changes from `aws_deploy` into the long-lived branch.
