# Батник файл для запуска

**run.bat**

```bat
@echo off
echo ===============================================
echo Java Annotations Demo - Build and Run
echo ===============================================

echo.
echo [1/4] Cleaning previous builds...
mvn clean

echo.
echo [2/4] Compiling project with annotation processing...
mvn compile

echo.
echo [3/4] Checking if generated classes were compiled...
if exist "target\classes\com\dandaev\edu\entities\PersonBuilder.class" (
    echo ✓ PersonBuilder.class found!
) else (
    echo ✗ PersonBuilder.class not found! Manually compiling generated sources...
    javac -cp target\classes -d target\classes target\generated-sources\com\dandaev\edu\entities\*.java
)

if exist "target\classes\com\dandaev\edu\entities\PersonToStringHelper.class" (
    echo ✓ PersonToStringHelper.class found!
) else (
    echo ✗ PersonToStringHelper.class not found! Manually compiling generated sources...
    javac -cp target\classes -d target\classes target\generated-sources\com\dandaev\edu\entities\*.java
)

echo.
echo [4/4] Running the example...
java -cp target\classes com.dandaev.edu.CodeGenerationExample

echo.
echo ===============================================
echo Done!
echo ===============================================
pause
```

**quick-run.bat** (быстрый запуск без очистки)

```bat
@echo off
echo Quick Run - Java Annotations Demo

echo Compiling...
mvn compile -q

echo Running...
java -cp target\classes com.dandaev.edu.CodeGenerationExample

pause
```

## Что нужно сделать перед запуском

### 1. Проверить структуру проекта

Убедитесь, что у вас такая структура:
```
java-annotations-demo/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── dandaev/
│       │           └── edu/
│       │               ├── annotation/
│       │               │   └── processor/
│       │               │       └── AdvancedAnnotationProcessor.java
│       │               ├── annotations/
│       │               │   ├── DefaultValue.java
│       │               │   ├── GenerateBuilder.java
│       │               │   └── GenerateToString.java
│       │               ├── entities/
│       │               │   └── Person.java
│       │               └── CodeGenerationExample.java
│       └── resources/
│           └── META-INF/
│               └── services/
│                   └── javax.annotation.processing.Processor
├── target/
│   ├── generated-sources/    ← сгенерированные файлы
│   └── classes/              ← скомпилированные классы
├── pom.xml
└── run.bat                   ← этот батник
```

### 2. Обновить pom.xml

Убедитесь, что в pom.xml есть конфигурация для обработки сгенерированных источников:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.dandaev.edu</groupId>
    <artifactId>java-annotations-demo</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <generatedSourcesDirectory>target/generated-sources/annotations</generatedSourcesDirectory>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>add-source</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>add-source</goal>
                        </goals>
                        <configuration>
                            <sources>
                                <source>target/generated-sources</source>
                            </sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. Проверить CodeGenerationExample.java

Убедитесь, что код выглядит так:

```java
package com.dandaev.edu;

import com.dandaev.edu.entities.Person;
import com.dandaev.edu.entities.PersonBuilder;
import com.dandaev.edu.entities.PersonToStringHelper;
import java.util.Arrays;

public class CodeGenerationExample {
    public static void main(String[] args) {
        // Использование сгенерированного builder
        PersonBuilder builder = new PersonBuilder()
            .name("John")
            .age(25)
            .address("123 Main St")
            .hobbies(Arrays.asList("Reading", "Sports"));

        Person person = builder.build();

        // Использование сгенерированного toString
        String str = PersonToStringHelper.toString(person);
        System.out.println("Generated toString: " + str);
        System.out.println("Builder pattern ready to use");
    }
}
```

## Пошаговая инструкция

1. **Скачайте батник** - сохраните код выше в файл `run.bat` в корне проекта
2. **Откройте командную строку** в папке проекта
3. **Запустите батник**:
4.

   ```cmd

   run.bat
   ```

5. **Или вручную**:
6.

   ```cmd
   mvn clean compile
   java -cp target\classes com.dandaev.edu.CodeGenerationExample
   ```

## Если возникнут проблемы

**debug.bat** (для отладки)

```bat
@echo off
echo DEBUG MODE - Java Annotations Demo

echo [1] Clean...
mvn clean

echo [2] Compile with verbose output...
mvn compile -X

echo [3] Check generated files...
dir target\generated-sources /s
dir target\classes\com\dandaev\edu\entities /s

echo [4] Try to run...
java -cp target\classes com.dandaev.edu.CodeGenerationExample

pause
```

## Ожидаемый результат при успешном запуске

```
Generated toString: Person{name=John, age=25, address=123 Main St, hobbies=[Reading, Sports]}
Builder pattern ready to use
```

**Просто положите `run.bat` в корень проекта и запустите его!** 🚀
