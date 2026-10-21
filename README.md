# Mezcla de k listas ordenadas

**Grupo 7 – Estructuras de Datos**

Animación didáctica en Java que muestra, paso a paso, cómo se mezclan k listas ordenadas en una sola lista ordenada.

## ¿De qué trata el algoritmo?

Es un algoritmo **voraz**: en cada paso se toma el menor de los nodos cabeza de las k listas y se enlaza al final del resultado, sin volver atrás. Para encontrar ese menor rápidamente se usa un Min-heap que guarda como máximo una cabeza por lista.

- **Tiempo:** O(N log k)
- **Espacio extra:** O(k)

(N = nodos en total, k = número de listas). El programa incluye una pestaña con la explicación de la complejidad (mejor caso, caso promedio y peor caso).

## Estructuras de datos usadas

- **Listas enlazadas**: cada lista de entrada y la lista resultante.
- **Min-heap** (`PriorityQueue`): guarda las cabezas actuales y entrega la menor.

## Cómo ejecutarlo

Requiere Java 8 o superior.

```
javac MezclaKListas.java
java MezclaKListas
```
Universidad Nacional de Colombia - Sede Bogotá
2026
## Créditos

Programa creado con la asistencia de **Claude** (Anthropic), modelo **Claude Sonnet 5.5**.
