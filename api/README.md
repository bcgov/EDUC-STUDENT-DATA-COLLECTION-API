# EDUC-STUDENT-DATA-COLLECTION-API
## Build Setup

``` bash
#Prepare to run
- Connect to VPN
- start up nats-server using `docker run -d --name=nats-main -p 4222:4222 -p 6222:6222 -p 8222:8222 nats -js`

#Run application with local properties
mvn clean install -Dspring.profiles.active=dev

#Run application with default properties
mvn clean install

