**Ping-PongParent**

  Java version : JDK 17

  Springboot version :  3.14

  Spring-webFlux: 6.14

  Version Control : Maven


**项目结构**

Ping-PongParent

  ├── PingService -- Ping 微服务

  ├──PongService -- Pong 微服务


**搭建步骤**

  1、克隆项目，并导入到IDEA中完成编译


**测试步骤**

  1、Spock本地测试

     Ping测试类PingServiceTest.groovy
   
     Pong测试类PongServiceControllerTest.groovy
   

  2、Spock 联调测试

    1）分别启动三个Ping服务，定义端口：8082、80838、8084

    启动建议

    Java -Dserver.port=8082 -Dlogging.file.name=./logs/app-8082.log -jar PingClient-0.0.1-SNAPSHOT.jar


    java -Dserver.port=8083 -Dlogging.file.name=./logs/app-8083.log -jar PingClient-0.0.1-SNAPSHOT.jar


    java -Dserver.port=8084 -Dlogging.file.name=./logs/app-8084.log -jar PingClient-0.0.1-SNAPSHOT.jar

  2）启动Pong服务

  3）使用PingServiceRealRequsetTests.groovy进行测试验证
