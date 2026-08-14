# ShoppingBuddy
A peer-to-peer shopping platform for connecting buyers and independent shoppers, without the need for store partnership or involvement. Created as a graduation project for the Diploma in Science in Computing at the National College of Ireland.

## Runtime baseline

- Java 25 LTS
- Spring Boot 3.5.16
- MySQL
- Maven Wrapper 3.9.10

## Verify the build

Start the local MySQL service, then run:

```powershell
docker compose up -d
.\mvnw.cmd clean verify
```
