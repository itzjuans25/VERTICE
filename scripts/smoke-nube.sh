#!/usr/bin/env bash
set -Eeuo pipefail
paso=Inicio
trap 'printf "FALLA  %s\n" "$paso" >&2' ERR
(( $# == 7 )) || { echo 'Uso: smoke-nube.sh FRONTEND AUTH CATALOGO NOTIFICACIONES SUBASTAS PRODUCTO_ID LOTE_ID' >&2; exit 2; }
command -v curl >/dev/null
command -v jq >/dev/null
for url in "$1" "$2" "$3" "$4" "$5"; do [[ "$url" =~ ^https?://[^[:space:]/]+ ]]; done
f=${1%/}; a=${2%/}; c=${3%/}; n=${4%/}; s=${5%/}; elegido=$6; lote=$7
[[ "$elegido" =~ ^[1-9][0-9]*$ && "$lote" =~ ^[1-9][0-9]*$ ]]

esperar() {
  local limite=$((SECONDS+60)) dato resto
  while (( SECONDS < limite )); do
    resto=$((limite-SECONDS)); if (( resto > 5 )); then resto=5; fi
    if dato=$(curl -fsS --max-time "$resto" "$1" 2>/dev/null); then
      if [[ "$2" == web ]]; then
        if grep -q VÉRTICE <<<"$dato"; then return; fi
      elif jq -e '.status == "UP"' <<<"$dato" >/dev/null 2>&1; then return; fi
    fi
    if (( SECONDS+2 < limite )); then sleep 2; fi
  done
  return 1
}
paso=Frontend; esperar "$f/" web; echo "OK  $paso"
paso='Auth /health'; esperar "$a/health" api; echo "OK  $paso"
paso='Catálogo /health'; esperar "$c/health" api; echo "OK  $paso"
paso='Notificaciones /health'; esperar "$n/health" api; echo "OK  $paso"
paso='Subastas /health'; esperar "$s/health" api; echo "OK  $paso"

marca="$(date +%s)-$RANDOM-$RANDOM"
cuenta=$(jq -nc --arg email "smoke-$marca@example.test" --arg clave "Smoke-!$marca" '{email:$email,password:$clave}')
a="$a/api/ecommerce/usuario"; c="$c/api/ecommerce"
http=(-fsS --max-time 20); json=(-H 'Content-Type: application/json')
paso=Registro
registro=$(jq -c '. + {nombre:"Smoke VÉRTICE"}' <<<"$cuenta")
r=$(curl "${http[@]}" "${json[@]}" -d "$registro" "$a/save")
jq -e '.id > 0' <<<"$r" >/dev/null
echo "OK  $paso"
paso=Login
r=$(curl "${http[@]}" "${json[@]}" -d "$cuenta" "$a/login")
token=$(jq -er '.token | select(length > 0)' <<<"$r")
auth=(-H "Authorization: Bearer $token")
echo "OK  $paso"
paso='Catálogo con stock'
r=$(curl "${http[@]}" "$c/producto/explorar?tamano=48")
jq -e '.items | length > 0' <<<"$r" >/dev/null
if [[ -n "$elegido" ]]; then
  [[ "$elegido" =~ ^[1-9][0-9]*$ ]]
  r=$(curl "${http[@]}" "$c/producto/$elegido")
  id=$(jq -er 'select(.stock > 0) | .id' <<<"$r")
else
  id=$(jq -er '[.items[] | select(.stock > 0)][0].id // empty' <<<"$r")
fi
echo "OK  $paso"
paso='Agregar al carrito'
datos=$(jq -nc --argjson id "$id" '{productoId:$id,cantidad:1}')
r=$(curl "${http[@]}" "${auth[@]}" "${json[@]}" -d "$datos" "$c/carrito/agregar")
jq -e --argjson id "$id" 'any(.items[]; .productoId == $id)' <<<"$r" >/dev/null
echo "OK  $paso"
paso='Confirmar compra'
r=$(curl "${http[@]}" "${auth[@]}" -X POST "$c/compras/realizar")
p=$(jq -er 'select(.estado == "COMPLETADA") | .id | select(. > 0)' <<<"$r")
printf 'OK  %s. Pedido sintético %s\n' "$paso" "$p"
paso=Puja
s="$s/api/ecommerce/subastas/$lote"
r=$(curl "${http[@]}" "$s")
monto=$(jq -er '.lote | select(.estado == "ACTIVA") | .pujaMinima' <<<"$r")
datos=$(jq -nc --argjson monto "$monto" '{monto:$monto}')
r=$(curl "${http[@]}" "${auth[@]}" "${json[@]}" -d "$datos" "$s/pujas")
jq -e '.voyGanando == true' <<<"$r" >/dev/null
printf 'OK  %s. Lote %s\n' "$paso" "$lote"

