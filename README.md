# DeepBlue Rescue

## Descripción

DeepBlue Rescue es una aplicación desarrollada con Java 25 y Spring Boot 4 para gestionar información relacionada con el rescate y la atención de animales marinos.

El proyecto implementa persistencia de datos mediante Spring Data JPA y PostgreSQL. La estructura de la base de datos es creada y evolucionada exclusivamente con Flyway, mientras que Hibernate se utiliza para validar que las entidades coincidan con el esquema existente. También dispone de una capa de servicio encargada de aplicar reglas de negocio, controlar transacciones y transformar entidades en DTOs.

Las pruebas de integración se ejecutan sobre una instancia real y temporal de PostgreSQL creada mediante Testcontainers.

## Tecnologías utilizadas

* Java 25.
* Spring Boot 4.1.1.
* Maven Wrapper.
* Spring Data JPA.
* Hibernate.
* PostgreSQL.
* Flyway.
* MapStruct 1.6.3.
* Testcontainers.
* JUnit 5.
* Mockito.
* AssertJ.
* Docker Desktop.

## Modelo de datos

El sistema contiene las siguientes entidades:

| Entidad         | Descripción                                                              |
| --------------- | ------------------------------------------------------------------------ |
| `RescueCenter`  | Representa un centro encargado de recibir y atender animales rescatados. |
| `RescueCase`    | Almacena la información de un caso de rescate.                           |
| `Animal`        | Representa al animal asociado con un caso de rescate.                    |
| `MedicalRecord` | Contiene la información médica del animal.                               |
| `Specialist`    | Representa al especialista que atiende a los animales.                   |
| `Expertise`     | Representa las áreas de conocimiento de los especialistas.               |
| `Treatment`     | Registra los tratamientos realizados a los animales.                     |

También se utilizan las enumeraciones:

* `RescueStatus`: estado del caso de rescate.
* `AnimalSex`: sexo del animal.
* `TreatmentType`: tipo de tratamiento aplicado.

## Relaciones entre las entidades

* Un `RescueCenter` puede tener muchos `RescueCase`.
* Cada `RescueCase` pertenece a un solo `RescueCenter`.
* Un `RescueCase` puede tener un solo `Animal`.
* Un `Animal` pertenece a un solo `RescueCase`.
* Un `Animal` puede tener un solo `MedicalRecord`.
* Un `MedicalRecord` pertenece a un solo `Animal`.
* Un `Specialist` puede tener varias áreas de experiencia.
* Un área de `Expertise` puede pertenecer a varios especialistas.
* Un `Animal` puede recibir muchos tratamientos.
* Un `Specialist` puede realizar muchos tratamientos.
* Cada `Treatment` pertenece a un animal y a un especialista.

La relación entre `Specialist` y `Expertise` es de muchos a muchos y se representa mediante la tabla asociativa `specialist_expertise`.

## Migraciones con Flyway

Flyway es el único responsable de crear y modificar el esquema de la base de datos.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration
```

El proyecto contiene las siguientes migraciones:

### V1__create_schema.sql

Crea las tablas principales, sus claves primarias, claves foráneas, restricciones `UNIQUE`, restricciones `CHECK` e índices.

### V2__insert_expertise_catalog.sql

Inserta el catálogo inicial de áreas de experiencia:

* Marine Reptiles.
* Marine Mammals.
* Marine Birds.
* Trauma.
* Rehabilitation.
* Toxicology.

### V3__add_tracking_device_to_animal.sql

Agrega la columna opcional `tracking_device_code` a la tabla `animals` y establece una restricción `UNIQUE` para impedir que dos animales tengan el mismo código de dispositivo.

Las migraciones existentes no deben modificarse después de haber sido aplicadas. Cualquier cambio posterior del esquema debe realizarse mediante una migración nueva.

## Configuración de Hibernate

La configuración de JPA utiliza:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

El valor `validate` hace que Hibernate compare las entidades con el esquema creado por Flyway. Hibernate no crea ni modifica las tablas.

No se utiliza `ddl-auto: create` ni `ddl-auto: update`, porque la administración del esquema corresponde exclusivamente a Flyway.

## Repositorios

Los repositorios extienden `JpaRepository`, por lo cual heredan operaciones como:

* `save`.
* `saveAll`.
* `findById`.
* `findAll`.
* `existsById`.
* `count`.
* `delete`.
* `flush`.
* `saveAndFlush`.

## Query Methods

Se utilizaron Query Methods cuando las consultas podían expresarse claramente mediante el nombre del método.

### RescueCenterRepository

* Buscar un centro por su código.

```java
findByCode(String code)
```

### RescueCaseRepository

* Buscar un caso por su código.
* Buscar casos por estado ordenados por fecha de rescate.
* Buscar casos por el código del centro.
* Buscar casos posteriores a una fecha, ordenados de forma descendente.

```java
findByCaseCode(String caseCode)
findByStatusOrderByRescueDateAsc(RescueStatus status)
findByRescueCenterCode(String centerCode)
findByRescueDateAfterOrderByRescueDateDesc(LocalDate date)
```

### AnimalRepository

* Buscar un animal por su código.
* Buscar animales cuyo nombre común contenga un texto.
* Buscar animales según el estado del caso.
* Buscar animales por el código del centro de rescate.

```java
findByAnimalCode(String animalCode)
findByCommonNameContainingIgnoreCase(String commonName)
findByRescueCaseStatus(RescueStatus status)
findByRescueCaseRescueCenterCode(String centerCode)
```

### ExpertiseRepository

* Buscar un área de experiencia por su nombre sin diferenciar mayúsculas y minúsculas.

```java
findByNameIgnoreCase(String name)
```

### TreatmentRepository

* Buscar los tratamientos de un animal ordenados cronológicamente.

```java
findByAnimalIdOrderByPerformedAtAsc(Long animalId)
findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode)
```

## Consultas JPQL

JPQL se utilizó únicamente en las consultas que requerían relaciones más complejas entre las entidades.

Las consultas implementadas permiten:

* Buscar especialistas activos según un área de experiencia.
* Buscar tratamientos realizados entre dos fechas.
* Buscar tratamientos relacionados con un centro de rescate.
* Buscar tratamientos realizados por especialistas con determinada experiencia.
* Buscar animales por el estado del caso y por el área de experiencia del especialista que realizó el tratamiento.

Estas consultas utilizan los nombres de las entidades y sus atributos. No se utiliza SQL nativo dentro de los repositorios.

## Pruebas de integración

Las pruebas se encuentran en:

```text
src/test/java/com/deepblue/rescue/PersistenceIntegrationIT.java
```

Se implementaron 15 pruebas de integración para verificar:

* La ejecución correcta de las migraciones V1, V2 y V3.
* Los métodos heredados de `JpaRepository`.
* La relación entre centros y casos de rescate.
* La relación entre un caso y un animal.
* La relación entre un animal y su historia médica.
* La relación muchos a muchos entre especialistas y áreas de experiencia.
* Los Query Methods.
* Las consultas JPQL.
* El orden cronológico de los tratamientos.
* Las consultas por intervalos de fechas.
* La restricción única del código del animal.
* Las restricciones de claves foráneas.
* La restricción `CHECK` del estado de rescate.
* La persistencia y consulta de un escenario completo.
* La consulta que relaciona animales, casos, tratamientos, especialistas y áreas de experiencia.

## Testcontainers

Las pruebas utilizan Testcontainers para iniciar automáticamente una instancia temporal de PostgreSQL mediante la imagen:

```text
postgres:18-alpine
```

Para ejecutar las pruebas es necesario que Docker Desktop se encuentre abierto.

Testcontainers permite:

* Probar la aplicación con PostgreSQL real.
* Crear un entorno aislado para cada ejecución.
* Evitar depender de una base de datos instalada y configurada manualmente.
* Eliminar automáticamente el contenedor al finalizar las pruebas.
* Verificar el comportamiento real de las restricciones de PostgreSQL.

No se utiliza H2.

Durante la ejecución se pueden observar los contenedores temporales con:

```powershell
docker ps
```

## Ejecución de las pruebas

Las pruebas de integración se mantienen separadas de las pruebas unitarias del
nuevo laboratorio. Desde la carpeta principal del proyecto, con Docker Desktop
iniciado, ejecutar:

```powershell
.\mvnw.cmd "-Dtest=PersistenceIntegrationIT" test
```

El resultado esperado es:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Ejecución de la aplicación

Para ejecutar la aplicación se necesita una instancia disponible de PostgreSQL.

La configuración admite las siguientes variables de entorno:

* `DB_URL`.
* `DB_USER`.
* `DB_PASSWORD`.

Si no se proporcionan, se utilizan los valores definidos por defecto en `application.yml`.

En Windows, la aplicación puede iniciarse con:

```powershell
.\mvnw.cmd spring-boot:run
```

Al iniciar, Flyway aplica las migraciones pendientes y Hibernate valida que las entidades coincidan con el esquema.

## Validación de ddl-auto

Para comprobar el funcionamiento de `ddl-auto: validate`, se cambió temporalmente el nombre de una columna en la entidad `Animal`, haciendo que no coincidiera con la columna creada por Flyway.

Como resultado, el contexto de Spring no pudo iniciar y las pruebas produjeron `BUILD FAILURE`. Esto confirmó que Hibernate estaba validando correctamente el esquema.

Después de restaurar el nombre correcto de la columna, las 15 pruebas finalizaron con `BUILD SUCCESS`.

## Respuestas de análisis del modelo

### 1. ¿Dónde se encuentra la clave foránea entre RescueCenter y RescueCase?

La clave foránea se encuentra en la columna `rescue_center_id` de la tabla `rescue_cases`. Esta columna referencia la clave primaria `id` de la tabla `rescue_centers`.

### 2. ¿Dónde se encuentra la clave foránea entre Animal y MedicalRecord?

La clave foránea se encuentra en la columna `animal_id` de la tabla `medical_records`. Esta columna referencia la clave primaria `id` de la tabla `animals`.

### 3. ¿Qué elemento garantiza que la relación entre Animal y MedicalRecord sea uno a uno?

La restricción `UNIQUE` aplicada sobre `medical_records.animal_id` impide que existan dos historias médicas relacionadas con el mismo animal. La clave foránea establece la relación y la restricción única garantiza que sea uno a uno.

### 4. ¿Por qué Specialist y Expertise necesitan una tabla intermedia?

Porque su relación es de muchos a muchos. Un especialista puede tener varias áreas de experiencia y una misma área puede corresponder a varios especialistas. La tabla `specialist_expertise` almacena las claves de ambas entidades.

### 5. ¿Cuáles son las claves foráneas de Treatment?

La tabla `treatments` contiene las claves foráneas `animal_id` y `specialist_id`. Estas columnas relacionan cada tratamiento con el animal que lo recibió y con el especialista que lo realizó.

### 6. ¿Puede existir un Treatment sin un Animal?

No. La columna `animal_id` está definida como `NOT NULL` y es una clave foránea que referencia la tabla `animals`.

### 7. ¿Puede existir un Treatment sin un Specialist?

No. La columna `specialist_id` está definida como `NOT NULL` y es una clave foránea que referencia la tabla `specialists`.

## Decisiones importantes del modelo

### Clave primaria compuesta

La tabla `specialist_expertise` utiliza una clave primaria compuesta por `specialist_id` y `expertise_id`. Esto evita registrar dos veces la misma relación entre un especialista y un área de experiencia.

### Índices

No fue necesario crear índices adicionales para las claves primarias porque PostgreSQL las indexa automáticamente.

Se agregaron índices para apoyar consultas frecuentes sobre:

* Centro de rescate.
* Estado del caso.
* Fecha de rescate.
* Animal relacionado con un tratamiento.
* Especialista relacionado con un tratamiento.
* Fecha de realización del tratamiento.

### Propietarios de las relaciones uno a uno

* `Animal` es el propietario de la relación entre `RescueCase` y `Animal`, porque contiene el `@JoinColumn`.
* `MedicalRecord` es el propietario de la relación entre `Animal` y `MedicalRecord`, porque contiene el `@JoinColumn`.

### Métodos auxiliares bidireccionales

Los métodos auxiliares permiten actualizar los dos lados de una relación dentro de la memoria antes de guardar las entidades. Por ejemplo, al asociar un caso con un centro, tanto el caso como la colección del centro conservan la misma información.

### Restricción CHECK

Aunque el estado se representa mediante un enum de Java, la restricción `CHECK` sigue siendo necesaria en PostgreSQL. De esta manera, la base de datos también protege la integridad de los datos cuando recibe información desde otro programa o mediante una operación manual.

### nullable = false y claves foráneas

`nullable = false` expresa en el modelo JPA que una relación es obligatoria. La restricción `NOT NULL` y la clave foránea de PostgreSQL ofrecen la protección definitiva dentro de la base de datos.

## Reglas respetadas

* Flyway crea y modifica el esquema.
* Hibernate solamente valida el esquema.
* Se utiliza `ddl-auto: validate`.
* Las pruebas utilizan PostgreSQL mediante Testcontainers.
* No se utiliza H2.
* No se utiliza SQL nativo en los repositorios.
* Se priorizan los Query Methods.
* Las consultas complejas utilizan JPQL.
* No se utiliza Lombok `@Data` en las entidades.
* Se implementaron DTOs, mappers y servicios sin agregar controladores ni una API REST.
* No se implementaron seguridad, frontend, Kafka ni Docker Compose.

## Conclusión

El laboratorio permitió implementar y comprobar una capa de persistencia completa para DeepBlue Rescue. Flyway administra la evolución del esquema, JPA representa las entidades y sus relaciones, Spring Data facilita las consultas y Testcontainers permite verificar el funcionamiento del sistema sobre PostgreSQL real.

Las pruebas confirman que las relaciones, consultas, migraciones y restricciones de integridad funcionan correctamente. La capa de servicio amplía el proyecto mediante reglas de negocio y pruebas unitarias aisladas de la base de datos.

## Capa de servicio

La capa de servicio se encuentra en:

```text
src/main/java/com/deepblue/rescue/service
```

Se implementaron los siguientes servicios:

* `RescueCaseService`: consulta casos y controla las transiciones de estado.
* `TreatmentService`: registra y consulta tratamientos.
* `AnimalService`: consulta animales y determina si pueden recibir tratamientos.

Las implementaciones utilizan inyección por constructor. Las consultas se ejecutan con `@Transactional(readOnly = true)` y las operaciones que modifican datos utilizan `@Transactional`.

## DTOs y mappers

Los objetos de transferencia se definieron mediante `record` y se encuentran en los paquetes `dto.request` y `dto.response`.

MapStruct transforma las entidades en respuestas sin exponer directamente las relaciones JPA:

```text
RescueCase -> RescueCaseResponse
Treatment  -> TreatmentResponse
Animal     -> AnimalResponse
```

## Reglas de negocio implementadas

### Transiciones de un caso de rescate

El flujo permitido es:

```text
ADMITTED
-> UNDER_EVALUATION
-> IN_REHABILITATION
-> READY_FOR_RELEASE
-> RELEASED
```

Una transición que no siga este orden produce `BusinessRuleException` y no se guarda en la base de datos.

### Registro de tratamientos

Antes de registrar un tratamiento se comprueba que:

1. El animal exista.
2. El especialista exista.
3. El especialista esté activo.
4. El caso no esté `RELEASED` ni `CLOSED`.
5. La fecha del tratamiento no sea anterior a la fecha del rescate.

Los recursos inexistentes producen `ResourceNotFoundException`. Las operaciones prohibidas por el negocio producen `BusinessRuleException`.

## Pruebas unitarias de los servicios

Las pruebas unitarias se encuentran en:

```text
src/test/java/com/deepblue/rescue/service
```

Se implementaron 18 pruebas unitarias con JUnit, Mockito y AssertJ:

* 5 pruebas para `RescueCaseServiceImpl`.
* 7 pruebas para `TreatmentServiceImpl`.
* 6 pruebas para `AnimalServiceImpl`.

Estas pruebas utilizan repositorios y mappers simulados. No necesitan Spring ApplicationContext, PostgreSQL, Hibernate ni Testcontainers.

En Windows se ejecuta el laboratorio de servicios completo con:

```powershell
.\mvnw.cmd clean test
```

El resultado esperado es `Tests run: 18` y `BUILD SUCCESS`. Estas pruebas no
inician Spring, PostgreSQL ni Testcontainers, por lo que Docker no es necesario.

Para ejecutar por separado las 15 pruebas de integración del laboratorio
anterior, Docker Desktop debe estar iniciado:

```powershell
.\mvnw.cmd "-Dtest=PersistenceIntegrationIT" test
```

## Respuestas de análisis de la capa de servicio

### Clasificación de responsabilidades

| Necesidad | Capa responsable |
|---|---|
| Ejecutar el `SELECT` de un caso por código | Repository |
| Validar una transición de estado | Service |
| Convertir un caso de rescate en DTO | Mapper |
| Guardar un tratamiento | Repository, coordinado por Service |
| Verificar que el especialista esté activo | Service |
| Crear la tabla `treatments` | Flyway |
| Controlar una transacción | Service |
| Representar información persistente | Entity |

### ¿Por qué la validación del especialista activo no pertenece al Repository?

Porque el Repository se encarga de acceder a los datos. La decisión de permitir o rechazar un tratamiento según el estado del especialista es una regla del negocio y debe permanecer en el Service.

### ¿Qué operaciones son de solo lectura?

`findByCode`, `findByStatus`, `findByAnimalCode` y las consultas de `AnimalService` utilizan `@Transactional(readOnly = true)`. `register` y `changeStatus` necesitan una transacción de escritura porque guardan modificaciones.

### ¿Por qué es peligroso utilizar `Optional.get()`?

Porque lanza `NoSuchElementException` cuando el valor no existe y no explica qué recurso faltó. `orElseThrow()` permite lanzar una excepción específica con un mensaje entendible.

### ¿Por qué no se retornan entidades directamente?

Retornar entidades aumenta el acoplamiento entre capas, puede activar relaciones `LAZY` fuera de una transacción y podría exponer información que el consumidor no necesita. Los DTOs crean un contrato más estable y permiten modificar el modelo persistente sin afectar automáticamente las demás capas.

### Diferencia entre Entity y DTO

Una Entity representa información persistente y sus relaciones con la base de datos. Un DTO transporta únicamente la información que otra capa necesita.

### ¿Por qué las reglas de tratamiento pertenecen al Service?

Porque registrar un tratamiento requiere coordinar varios repositorios, validar el estado del especialista y del caso, comprobar fechas y controlar una transacción completa. El Repository solo debe consultar o guardar información.
