INSERT INTO productos (id, nombre, precio) VALUES
    (1, 'Teclado mecanico', 250000),
    (2, 'Monitor 27 pulgadas', 900000),
    (3, 'Mouse inalambrico', 80000),
    (4, 'Cable USB-C', 15000);

INSERT INTO inventario (producto_id, stock) VALUES
    (1, 50),
    (2, 10),
    (3, 100),
    (4, 2);

-- El cliente 5 tiene NIT: se usa en la Parte 2 (campaña CORPORATIVO).
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES
    (1, 'Ana Torres', 'VIP', NULL),
    (2, 'Luis Gomez', 'FRECUENTE', NULL),
    (3, 'Carlos Ruiz', 'MOROSO', NULL),
    (4, 'Maria Perez', 'ESTANDAR', NULL),
    (5, 'Comercializadora Andina SAS', 'ESTANDAR', '900123456');

-- Carlos debe 300.000 (la factura pagada no cuenta como deuda).
INSERT INTO facturas (cliente_id, monto, pagada) VALUES
    (3, 300000, FALSE),
    (3, 120000, TRUE);

-- Luis tiene 12 pedidos previos: más de 10, así que le corresponde el 8 %.
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES
    (2, 100000, 0, 19000, 119000, TIMESTAMP '2026-01-10 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0, 19000, 119000, TIMESTAMP '2026-01-24 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0, 19000, 119000, TIMESTAMP '2026-02-07 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0, 19000, 119000, TIMESTAMP '2026-02-21 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-03-07 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-03-21 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-04-04 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-04-18 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-05-02 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-05-16 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.04, 18240, 114240, TIMESTAMP '2026-05-30 10:00:00', 'CONFIRMADO'),
    (2, 100000, 0.08, 17480, 109480, TIMESTAMP '2026-06-13 10:00:00', 'CONFIRMADO');
