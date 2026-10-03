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

#### Comportamiento de referencia (antes de refactorizar)

`ProcesarPedidoTest` fija la salida actual de `procesarPedido()`. Estas pruebas solo usan el
método público, así que servirán para comprobar que el refactor no cambia el comportamiento:

| Caso | Pedido | Resultado del código original |
|---|---|---|
| VIP, más de $1.000.000 | 2 monitores | Confirmado, total $1.820.700 (15 %) |
| FRECUENTE, más de 10 pedidos | 4 teclados | Confirmado, total $1.094.800 (8 %) |
| ESTANDAR | 1 teclado | Confirmado, total $297.500 |
| Stock insuficiente | 5 cables (hay 2) | Rechazado: "Stock insuficiente: producto 4" |
| Pedido sin ítems | lista vacía | Rechazado: "El pedido no contiene items" |
| Moroso a las 10:00 | 1 mouse | Rechazado: "Cliente con deuda pendiente: $300000.0" |
| Moroso a las 21:00 | 1 mouse | Confirmado, total $95.200 (excepción por horario) |
| Cliente inexistente | 1 mouse | `EmptyResultDataAccessException`: la rama "Cliente no registrado" (línea 56) nunca se ejecuta |

El último caso es un defecto, no una decisión de diseño: `queryForObject` lanza excepción cuando
no hay filas en lugar de devolver `null`. Se mantiene durante el refactor y se corrige después,
en un commit propio.

Siguiente paso: aplicar Chain of Responsibility a las validaciones, Strategy al descuento y
extraer la persistencia y la notificación.
