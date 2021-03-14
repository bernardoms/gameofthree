FROM openjdk:11

WORKDIR app

ADD target/*.jar /app/game-of-three.jar

CMD java $JAVA_OPTS -jar /app/game-of-three.jar