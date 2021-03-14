# gameofthree-socket

Game of Three

When a player starts, it incepts a random (whole) number and sends it to the second
player as an approach of starting the game. The receiving player can now always choose
between adding one of {-1, 0, 1} to get to a number that is divisible by 3. Divide it by three. The
resulting whole number is then sent back to the original sender.
The same rules are applied until one player reaches the number 1(after the division).

# Technologies 
- Spring Boot
- Java 8
- Socket

# How To Use

First Option:
- Clone the project and execute in IDE.

Second Option:
- Clone the project
- mvn clean package
- cd /target
- java -jar .\gameofthree-0.0.1-SNAPSHOT.jar

Third Option:
- Execute go https://game-of-three-socket.herokuapp.com/

Forth Option:
### Running local with everything on a container :

 `./mvnw clean package` 
 `cd deps`
 `docker-compose build`
 `docker-compose up -d`
 
 * You can edit the docker-compose yml and add a new-relic license key to see/monitoring the api at newrelic.
 * You can see logs on a local kibana at http://localhost:5601 just need to create an index on kibana for be able to 
 look at the logs.

# Tests
There are some tests inside src/test folder

