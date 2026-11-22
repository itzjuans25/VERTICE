SELECT json_build_object(
  'tablas', (SELECT json_agg(json_build_object('tabla',table_name,'columna',column_name,'tipo',data_type)
                             ORDER BY table_name, ordinal_position)
             FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name IN ('usuario','products','carrito','compras','item_carrito','sms_simulado','notificaciones_pendientes')),
  'fk', (SELECT json_agg(json_build_object('origen',tc.table_name,'columna',kcu.column_name,'destino',ccu.table_name,'referencia',ccu.column_name))
         FROM information_schema.table_constraints tc
         JOIN information_schema.key_column_usage kcu ON kcu.constraint_schema=tc.constraint_schema AND kcu.constraint_name=tc.constraint_name
         JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_schema=tc.constraint_schema AND ccu.constraint_name=tc.constraint_name
         WHERE tc.table_schema='public' AND tc.constraint_type='FOREIGN KEY'
           AND tc.table_name IN ('carrito','compras','item_carrito'))
);
