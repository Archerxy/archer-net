# mvn install to local repository
mvn install:install-file -DgroupId="com.archer" -DartifactId="archer-net" -Dversion="${version}" -Dpackaging=jar -Dfile=${path:}/archer-net-1.1.3.jar


# pom 
<dependency>
    <groupId>com.archer</groupId>
    <artifactId>archer-net</artifactId>
    <version>${version}</version>
</dependency>
