#!/bin/bash
sed -i '' -e 's/<\/project>//g' pom.xml
cat << 'INNER_EOF' >> pom.xml
    <!-- Optional Spring Security for extracting user info -->
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-core</artifactId>
      <scope>provided</scope>
    </dependency>
</project>
INNER_EOF
