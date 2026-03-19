# GiveLunch AWS Operations Checklist

## Pre-Deploy
- Confirm `aws_deploy` is up to date.
- Verify production env file exists at `/etc/givelunch/givelunch.env`.
- Verify RDS, Redis, SMTP, DataGoKr, and Naver credentials are valid.
- Confirm the EC2 instance can reach RDS on `3306`.
- Confirm the EC2 instance can reach Redis on `6379`.

## Manual Deploy
- Build the jar with `./mvnw clean verify`.
- Upload the jar to EC2.
- Run `deploy/scripts/deploy.sh <jar-path>`.
- Check `systemctl status givelunch`.
- Check Nginx status.
- Check `curl http://127.0.0.1/actuator/health`.

## Post-Deploy Smoke Test
- Load `/login`.
- Log in with a normal user account.
- Open `/roulette`.
- Trigger sign-up verification email.
- Trigger password reset email.
- Record one ranking action and verify Redis-backed ranking.
- Confirm external food and image lookups work.

## Rollback
- Run `deploy/scripts/rollback.sh` on EC2.
- Re-check `systemctl status givelunch`.
- Re-run health check and the core smoke test.

## Merge Checklist
- Tests pass on `aws_deploy`.
- Production profile starts with the real env file.
- Manual EC2 deployment completed once.
- GitHub Actions deployment completed once.
- CloudWatch logs are visible.
- No unrelated feature changes are included in the branch.
