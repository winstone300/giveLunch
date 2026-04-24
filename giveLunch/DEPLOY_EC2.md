# 단일 EC2 배포 가이드

이 문서는 giveLunch를 단일 EC2 인스턴스에 배포하는 절차입니다. 구성은 `EC2 + Java 21 JAR + systemd + Nginx + MySQL + Redis + SES SMTP` 기준입니다.

## 1. AWS 콘솔에서 할 일

### EC2

- Ubuntu 24.04 LTS 인스턴스를 생성합니다.
- 인스턴스 크기는 최소 `t3.small`, 권장 `t3.medium`입니다.
- EBS는 gp3 30GB 이상으로 시작합니다.
- Elastic IP를 할당하고 EC2에 연결합니다.

### 보안그룹

| 포트 | 소스 | 용도 |
| --- | --- | --- |
| 22 | 내 IP `/32` | SSH |
| 80 | `0.0.0.0/0`, `::/0` | HTTP, Certbot 인증 |
| 443 | `0.0.0.0/0`, `::/0` | HTTPS |

`8080`, `3306`, `6379`는 외부에 열지 않습니다. Spring Boot, MySQL, Redis는 EC2 내부에서만 접근합니다.

### DNS

- Route 53 또는 사용 중인 DNS에서 `app.example.com` 같은 서브도메인을 Elastic IP로 연결합니다.
- DNS가 전파된 뒤 Certbot으로 HTTPS 인증서를 발급합니다.

### SES

- SES에서 발신 도메인을 verified identity로 등록합니다.
- DKIM DNS 레코드를 등록합니다.
- SES sandbox 상태라면 production access를 요청합니다.
- SMTP credentials를 생성하고 `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`에 사용합니다.

## 2. 로컬에서 빌드

Windows PowerShell 기준입니다.

```powershell
cd D:\portPolio\giveLunch\giveLunch
.\mvnw.cmd test
.\mvnw.cmd package
```

서버로 업로드합니다.

```powershell
scp .\target\giveLunch-0.0.1-SNAPSHOT.jar ubuntu@EC2_PUBLIC_IP:/tmp/app.jar
scp .\deploy\ec2\givelunch.service ubuntu@EC2_PUBLIC_IP:/tmp/givelunch.service
scp .\deploy\ec2\nginx-givelunch.conf ubuntu@EC2_PUBLIC_IP:/tmp/nginx-givelunch.conf
scp .\deploy\ec2\givelunch.env.example ubuntu@EC2_PUBLIC_IP:/tmp/givelunch.env
scp .\deploy\ec2\mysql-backup.sh ubuntu@EC2_PUBLIC_IP:/tmp/mysql-backup.sh
scp .\src\main\resources\db\seed\demo_seed.sql ubuntu@EC2_PUBLIC_IP:/tmp/demo_seed.sql
```

## 3. EC2 기본 패키지 설치

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless mysql-server redis-server nginx certbot python3-certbot-nginx apache2-utils
java -version
```

## 4. OS 사용자와 배포 디렉터리

```bash
sudo useradd --system --home /opt/givelunch --shell /usr/sbin/nologin givelunch
sudo install -d -o givelunch -g givelunch /opt/givelunch
sudo install -d -m 750 -o root -g givelunch /etc/givelunch
sudo install -d -m 750 -o root -g root /var/backups/givelunch

sudo mv /tmp/app.jar /opt/givelunch/app.jar
sudo chown givelunch:givelunch /opt/givelunch/app.jar

sudo mv /tmp/givelunch.env /etc/givelunch/givelunch.env
sudo chown root:givelunch /etc/givelunch/givelunch.env
sudo chmod 640 /etc/givelunch/givelunch.env
```

`/etc/givelunch/givelunch.env`를 열어 실제 값을 채웁니다.

```bash
sudo nano /etc/givelunch/givelunch.env
```

반드시 바꿀 값:

- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_MAIL_FROM`
- `APP_AGENT_AUTH_API_KEY`
- `APP_DATA_GO_KR_SERVICE_KEY`
- `APP_NAVER_IMAGE_CLIENT_ID`
- `APP_NAVER_IMAGE_CLIENT_SECRET`

## 5. MySQL 설정

```bash
sudo mysql
```

```sql
CREATE DATABASE givelunch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'givelunch'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON givelunch.* TO 'givelunch'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

`CHANGE_ME_STRONG_PASSWORD` 값을 `/etc/givelunch/givelunch.env`의 `SPRING_DATASOURCE_PASSWORD`와 맞춥니다.

Flyway migration은 앱 첫 기동 시 자동 실행됩니다. 그 뒤 JPA가 `ddl-auto: validate`로 스키마를 검증합니다.

## 6. Redis 설정

단일 EC2 구성에서는 Redis를 localhost에만 바인딩합니다.

```bash
sudo sed -i 's/^bind .*/bind 127.0.0.1 ::1/' /etc/redis/redis.conf
sudo sed -i 's/^# maxmemory .*/maxmemory 256mb/' /etc/redis/redis.conf
sudo sed -i 's/^# maxmemory-policy .*/maxmemory-policy allkeys-lru/' /etc/redis/redis.conf
sudo systemctl enable redis-server
sudo systemctl restart redis-server
```

## 7. systemd 서비스 등록

```bash
sudo mv /tmp/givelunch.service /etc/systemd/system/givelunch.service
sudo systemctl daemon-reload
sudo systemctl enable givelunch
sudo systemctl start givelunch
sudo journalctl -u givelunch -f
```

헬스 체크:

```bash
curl http://127.0.0.1:8080/actuator/health
```

## 8. 데모 데이터

관리자 비밀번호용 BCrypt hash를 생성합니다.

```bash
htpasswd -bnBC 10 "" 'CHANGE_ME_ADMIN_PASSWORD' | tr -d ':\n'
```

출력된 hash를 `/tmp/demo_seed.sql`의 `@ADMIN_PASSWORD_BCRYPT` 값에 넣습니다. `@ADMIN_EMAIL`도 실제 관리자 이메일로 바꿉니다.

```bash
nano /tmp/demo_seed.sql
mysql -u givelunch -p givelunch < /tmp/demo_seed.sql
```

대안: 웹 회원가입으로 계정을 만든 뒤 아래 SQL로 관리자 권한만 올릴 수 있습니다.

```sql
UPDATE users SET role = 'ADMIN' WHERE user_name = '가입한_아이디';
```

## 9. Nginx와 HTTPS

도메인을 바꿉니다.

```bash
sudo sed -i 's/app.example.com/app.your-domain.com/g' /tmp/nginx-givelunch.conf
sudo mv /tmp/nginx-givelunch.conf /etc/nginx/sites-available/givelunch
sudo ln -s /etc/nginx/sites-available/givelunch /etc/nginx/sites-enabled/givelunch
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

DNS가 EC2 Elastic IP를 가리키는지 확인한 뒤 HTTPS 인증서를 발급합니다.

```bash
sudo certbot --nginx -d app.your-domain.com
sudo systemctl reload nginx
```

외부에서 확인합니다.

```bash
curl https://app.your-domain.com/actuator/health
```

## 10. 백업

```bash
sudo mv /tmp/mysql-backup.sh /usr/local/bin/givelunch-mysql-backup
sudo chmod 750 /usr/local/bin/givelunch-mysql-backup
sudo /usr/local/bin/givelunch-mysql-backup
```

매일 새벽 3시 실행:

```bash
sudo crontab -e
```

```cron
0 3 * * * /usr/local/bin/givelunch-mysql-backup >> /var/log/givelunch-mysql-backup.log 2>&1
```

S3 업로드까지 하려면 EC2 IAM role에 S3 쓰기 권한을 붙이고 `/etc/givelunch/givelunch.env`에 아래 값을 추가합니다.

```bash
GIVELUNCH_BACKUP_S3_URI=s3://your-bucket/givelunch/mysql
```

## 11. 장애 확인 명령

앱 로그:

```bash
sudo journalctl -u givelunch -n 200 --no-pager
sudo journalctl -u givelunch -f
```

서비스 상태:

```bash
sudo systemctl status givelunch
sudo systemctl status nginx
sudo systemctl status mysql
sudo systemctl status redis-server
```

포트 확인:

```bash
sudo ss -lntp
```

Nginx 설정 확인:

```bash
sudo nginx -t
```

MySQL 접속 확인:

```bash
mysql -u givelunch -p givelunch -e "SHOW TABLES;"
```

## 12. 재배포

로컬에서 새 JAR를 빌드한 뒤 EC2에 업로드합니다.

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
scp .\target\giveLunch-0.0.1-SNAPSHOT.jar ubuntu@EC2_PUBLIC_IP:/tmp/app.jar
```

EC2에서 교체합니다.

```bash
sudo systemctl stop givelunch
sudo mv /tmp/app.jar /opt/givelunch/app.jar
sudo chown givelunch:givelunch /opt/givelunch/app.jar
sudo systemctl start givelunch
sudo journalctl -u givelunch -f
```
