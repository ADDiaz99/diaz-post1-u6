# Post-contenido, Unidad 6: Antipatrones de Diseño

## Descripción

Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software, sexto semestre.
Un único proyecto Spring Boot (`pedidos-service`) con dos partes:

- **Parte 1:** diagnóstico y refactorización de un antipatrón combinado (God Object y Spaghetti
  Code) en `GestorPedidos`.
- **Parte 2:** diagnóstico y corrección de un segundo antipatrón (Golden Hammer) introducido al
  hacer crecer el mismo proyecto con tres campañas de descuento.

El historial de commits refleja el proceso completo: primero el código original con sus pruebas
de caracterización, luego el diagnóstico, después cada refactorización.

## Cómo ejecutar

Requisitos: Java 17 a 26 (desarrollado con Java 26) y Maven 3.9+.

```
$ mvn test
$ mvn spring-boot:run
```

Con la aplicación arriba, un pedido de prueba (cliente VIP, 2 monitores):

```
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":1,"clienteEmail":"ana@tienda.com","items":[{"productoId":2,"cantidad":2}]}'
```

Responde `201` con `"total":1820700.0`, o `422` con el motivo si el pedido se rechaza. El correo
de confirmación se imprime en la consola del servidor.

**Datos de prueba** (`data.sql`): clientes 1 VIP, 2 FRECUENTE (12 pedidos previos), 3 MOROSO
(debe $300.000), 4 ESTANDAR, 5 ESTANDAR con NIT; productos 1 teclado ($250.000), 2 monitor
($900.000), 3 mouse ($80.000) y 4 cable (solo 2 en stock).

## Estructura

```
src/main/java/com/tienda/pedidos/
├── controller/PedidoController.java       POST /api/pedidos
├── dto/                                   PedidoRequest, ItemPedido, ResultadoPedido
├── validacion/                            Chain of Responsibility
│   ├── ValidadorPedido.java               eslabón base
│   ├── ValidadorStock.java                ítems, producto y stock
│   ├── ValidadorCliente.java              cliente y mora con horario de corte
│   └── ContextoPedido.java
├── descuento/                             Strategy
│   ├── EstrategiaDescuento.java
│   ├── DescuentoVip / DescuentoFrecuente / DescuentoEstandar
│   └── SelectorEstrategiaDescuento.java   regla por tipo de cliente
├── service/
│   ├── GestorPedidos.java                 orquestador delgado
│   ├── PedidoRepository.java / ProductoRepository.java
│   ├── NotificacionPedidoService.java
│   └── EmailService.java / EmailServiceConsola.java
└── config/RelojConfig.java                reloj inyectable (horario de corte)
```


## Decisiones de diseño

### Parte 1: GestorPedidos

#### Antipatrón identificado: God Object y Spaghetti Code combinados

Las líneas citadas corresponden a `GestorPedidos.java` del primer commit
(`feat: implementar GestorPedidos...`), que es el código de la guía copiado sin corregir.

**God Object.** `procesarPedido()` (líneas 33 a 136, 104 líneas) concentra seis
responsabilidades más el registro de logs, cada una con su propia razón para cambiar:

| Responsabilidad | Líneas | Razón de cambio |
|---|---|---|
| Validación de ítems y stock | 36 a 49 | cambia la política de inventario |
| Validación de cliente y mora | 51 a 70 | cambia la política de cartera o el horario de corte |
| Cálculo de subtotal | 72 a 78 | cambia el origen de los precios |
| Descuento e impuesto | 80 a 100 | mercadeo crea o modifica un descuento; cambia el IVA |
| Persistencia | 102 a 116 | cambia el esquema o el motor de base de datos |
| Notificación | 118 a 132 | cambia el texto o el canal del correo |
| Registro | 34, 38, 46, 55, 64, 67, 130, 134 | cambia la política de logs |

Es la definición de violación del Principio de Responsabilidad Única: siete razones distintas
para modificar la misma clase. Además depende directamente de detalles (`JdbcTemplate`, SQL
literal) y no de abstracciones, lo que viola el Principio de Inversión de Dependencias. La guía
menciona 340 líneas porque el archivo original incluía seis métodos privados auxiliares que no
se reprodujeron; aquí el archivo tiene 137 líneas, pero la concentración de responsabilidades es
la misma, y eso, no el número de líneas, es lo que define al God Object.

**Spaghetti Code.** El flujo de control del mismo método es difícil de seguir:

- **Anidamiento:** el bloque de mora llega a **3 niveles** de condicionales (líneas 57, 61 y 63:
  tipo MOROSO, deuda mayor que cero y hora antes de las 20:00). El de descuento llega a 2
  (líneas 82 y 83, 90 y 93).
- **Complejidad ciclomática de 23** (22 puntos de decisión contando cada `&&` y `||`), más del
  doble del umbral de 10 que McCabe propuso como límite razonable.
- **5 puntos de retorno** distintos (líneas 39, 47, 56, 65 y 135).
- **Mezcla de niveles de abstracción** en la misma secuencia: 9 sentencias SQL literales
  (líneas 43, 53, 59, 76, 92, 104, 108, 111 y 114), reglas de negocio (83 a 97), formato de texto
  del correo (119 a 126) y manejo de errores (127 a 132).

**Prueba de fuego:** agregar un tipo de cliente con descuento propio exige insertar otra rama
`else if` dentro de las líneas 80 a 98, quizás con otra consulta SQL embebida, y releer las 104
líneas del método para asegurarse de no romper la validación de mora, que también compara
`tipoCliente` (línea 57). Ningún cambio de descuento se puede aislar del resto del flujo.

#### Patrones aplicados

- **Chain of Responsibility para las validaciones** (`ValidadorStock` → `ValidadorCliente`).
  Las validaciones tienen dependencia real de orden y necesitan corte anticipado: si no hay
  stock, no tiene sentido consultar la mora del cliente.
- **Strategy para el descuento por tipo de cliente** (`DescuentoVip`, `DescuentoFrecuente`,
  `DescuentoEstandar` y `SelectorEstrategiaDescuento`). Siempre aplica exactamente una regla,
  elegida por el tipo de cliente, sin orden entre ellas. El selector es un mapa: el `if/else`
  anidado desaparece.
- **Extraer Clase** para persistencia (`PedidoRepository`, `ProductoRepository`) y notificación
  (`NotificacionPedidoService`). `GestorPedidos` queda como orquestador de 74 líneas, sin SQL.

#### Alternativas descartadas

- **Lista de `Predicate<ContextoPedido>` en un `validarTodo()`:** evalúa todos los predicados
  aunque el primero ya haya fallado, y no permite que un validador decida no delegar. La cadena
  sí corta al primer rechazo.
- **El descuento como un eslabón más de la cadena:** las reglas de descuento no tienen orden
  entre sí ni necesitan cortar el flujo; modelarlas como cadena obligaría a un mecanismo
  artificial para que solo una "gane". Strategy con un mapa lo resuelve con menos indirección.
- **Un Facade sobre GestorPedidos:** ocultaría el problema a los clientes de la clase, pero
  `GestorPedidos` seguiría teniendo siete razones internas para cambiar. La única corrección real
  es separar responsabilidades.

#### Antes y después

| Métrica | Antes (`procesarPedido` original) | Después |
|---|---|---|
| Líneas del método | 104 | 22 |
| Complejidad ciclomática | 23 | 2 |
| Niveles máximos de anidamiento | 3 | 1 |
| Sentencias SQL dentro de `GestorPedidos` | 9 | 0 |
| Clases que hay que tocar para un tipo de cliente nuevo | `GestorPedidos` completo | 1 clase nueva + 1 entrada en el selector |

**Misma salida con el nuevo diseño.** Las pruebas de `ProcesarPedidoTest` solo usan el método
público `procesarPedido()`, así que valen para cualquier diseño interno. Pasan sin cambios antes
y después del refactor:

| Caso | Pedido | Resultado (antes y después) |
|---|---|---|
| VIP, más de $1.000.000 | 2 monitores | Confirmado, total $1.820.700 (15 %) |
| FRECUENTE, más de 10 pedidos | 4 teclados | Confirmado, total $1.094.800 (8 %) |
| ESTANDAR | 1 teclado | Confirmado, total $297.500 |
| Stock insuficiente | 5 cables (hay 2) | Rechazado: "Stock insuficiente: producto 4" |
| Pedido sin ítems | lista vacía | Rechazado: "El pedido no contiene items" |
| Moroso a las 10:00 | 1 mouse | Rechazado: "Cliente con deuda pendiente: $300000.0" |
| Moroso a las 21:00 | 1 mouse | Confirmado, total $95.200 (excepción por horario) |
| Cliente inexistente | 1 mouse | Antes y después del refactor: `EmptyResultDataAccessException`; tras la corrección: Rechazado "Cliente no registrado" |

#### Hallazgos al caracterizar el código

Escribir las pruebas antes de refactorizar destapó defectos que no eran de diseño, sino de
funcionamiento. Cada uno se trató por separado:

1. **La rama "Cliente no registrado" nunca se ejecutaba.** `queryForObject` lanza
   `EmptyResultDataAccessException` cuando no hay filas; no devuelve `null`. El refactor conservó
   ese comportamiento a propósito (un refactor no debe cambiar la salida), y la corrección va en
   su propio commit (`fix: rechazar cliente o producto inexistente...`), con `queryForList`.
2. **La versión de referencia de la guía se saltaba la validación de stock.**
   `encadenar()` devuelve el eslabón siguiente, y la guía asignaba ese valor de retorno como
   primer validador (`this.primerValidador = stock.encadenar(cliente)`). La cadena empezaba en
   `ValidadorCliente`. La prueba de stock insuficiente lo detectó; aquí la cadena arranca en
   `stock`.
3. **La versión de referencia de la guía perdía el rechazo de pedidos vacíos.** El
   `ValidadorStock` de la guía no revisaba la lista vacía, y un pedido sin ítems se confirmaba
   con total 0. Se recuperó la comprobación del código original.
4. **`CALL IDENTITY()` no existe en H2 2.x en modo normal.** Solo lo acepta el modo `LEGACY`
   (verificado en el código fuente de H2 2.4.240), por eso la URL de la base usa `MODE=LEGACY`.
5. **El original no era transaccional.** Si fallaba una inserción de detalle, el pedido quedaba
   a medias. `PedidoRepository.guardar()` es `@Transactional`, lo que además asegura que
   `CALL IDENTITY()` lea el id en la misma conexión del `INSERT`.
6. **La hora no era controlable.** El código original usaba `LocalTime.now()`, así que el caso
   "moroso fuera del horario de corte" solo se podía probar después de las 20:00. Se inyectó un
   `Clock` (único cambio al código copiado de la guía) y las pruebas fijan la hora.

### Parte 2: crecimiento del proyecto

#### Antipatrón identificado: Golden Hammer

Las tres campañas se agregaron como eslabones de la cadena de validación (commit
`feat: agregar 3 campanas de descuento como eslabones...`). La evidencia:

- **No tienen dependencia de orden.** `PromocionBlackFriday`, `PromocionCorporativo` y
  `PromocionVolumen` se pueden ejecutar en cualquier orden y el resultado es el mismo, porque
  `aplicarDescuentoCampana()` siempre conserva el mayor (`ContextoPedido.java`, líneas 32 a 34).
  Esa es justo la propiedad que **sí** justifica la cadena en `ValidadorStock` y
  `ValidadorCliente`: el stock debe revisarse antes que la mora.
- **Nunca cortan el flujo.** Ninguna de las tres llama a `rechazar()`: solo escriben
  (`PromocionBlackFriday.java` línea 20, `PromocionCorporativo.java` línea 21,
  `PromocionVolumen.java` línea 15). Heredan de `ValidadorPedido`, cuyo contrato es "decidir si
  el pedido continúa o se rechaza", y no validan nada.
- **Comparten un campo mutable.** Las tres compiten por escribir `descuentoCampana`
  (`ContextoPedido.java`, línea 13). Si mercadeo pidiera **sumar** campañas en vez de tomar la
  mayor, la regla tendría que vivir en `aplicarDescuentoCampana()` y depender del orden de
  llegada de los eslabones: la cadena no lo expresa sin ambigüedad.
- **El orquestador quedó acoplado a ambas cosas.** `GestorPedidos` encadena cinco eslabones
  (líneas 43 y 44) y luego combina a mano el descuento de Strategy con el de la cadena
  (línea 65): dos mecanismos para una misma pregunta, "¿qué descuento aplica?".
- **La razón fue "ya funcionó".** Chain of Responsibility resolvió bien las validaciones de la
  Parte 1, y se reutilizó porque los eslabones "ya sabían conectarse", no porque el problema
  nuevo tuviera forma de cadena. Eso es Golden Hammer: aplicar la herramienta conocida a un
  problema con otra forma.

Siguiente paso: mover las tres campañas a `EstrategiaDescuento` y eliminar los eslabones.

## Herramientas utilizadas

- Java 26, Spring Boot 4.1.1, Spring JDBC (JdbcTemplate), H2 Database, Maven 3.9
- JUnit, Spring Boot Test, Git, GitHub

Se usó Spring Boot 4.1.1 en lugar de 3.x porque es la primera línea con soporte oficial para
Java 26. Por la misma razón el starter web es `spring-boot-starter-webmvc`, su nombre en Boot 4.
