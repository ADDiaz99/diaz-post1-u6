-- Los DROP permiten que un segundo contexto de Spring (por ejemplo, el de una
-- clase de prueba con otras propiedades) recree la base sin error de tabla existente.
DROP TABLE IF EXISTS detalle_pedido;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS facturas;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS inventario;
DROP TABLE IF EXISTS productos;

CREATE TABLE productos (
    id      BIGINT PRIMARY KEY,
    nombre  VARCHAR(100) NOT NULL,
    precio  DOUBLE NOT NULL
);

CREATE TABLE inventario (
    producto_id BIGINT PRIMARY KEY,
    stock       INT NOT NULL
);

CREATE TABLE clientes (
    id           BIGINT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    tipo_cliente VARCHAR(20) NOT NULL,
    nit          VARCHAR(20)
);

CREATE TABLE facturas (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    monto      DOUBLE NOT NULL,
    pagada     BOOLEAN NOT NULL
);

CREATE TABLE pedidos (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    subtotal   DOUBLE,
    descuento  DOUBLE,
    impuesto   DOUBLE,
    total      DOUBLE,
    fecha      TIMESTAMP,
    estado     VARCHAR(20)
);

CREATE TABLE detalle_pedido (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id   BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad    INT NOT NULL
);
