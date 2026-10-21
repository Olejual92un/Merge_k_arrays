/*
 * Compilar:  javac MezclaKListas.java
 * Ejecutar:  java MezclaKListas
 *
 * Usa solo la libreria estandar de Java (Swing y Java2D). Requiere Java 8 o superior.
 */
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MezclaKListas {

    // ---------------------------------------------------------------------
    //  Constantes: lienzo logico, limites y paleta
    // ---------------------------------------------------------------------
    static final int W = 1280, H = 720;                    
    static final int MAX_LISTAS = 5, MAX_POR_LISTA = 5, MAX_TOTAL = 20;

    static final Color FONDO = new Color(0x0D1B4A);     
    static final Color TEXTO = new Color(0xEAF0FF);
    static final Color TENUE = new Color(0x8FA3DB);
    static final Color CIAN = new Color(0x00D0FF);
    static final Color ROSA = new Color(0xFF2DAF);
    static final Color MENTA = new Color(0x5CF2A0);
    static final Color AMBAR = new Color(0xFFC83D);
    static final Color ROJO = new Color(0xFF4D5E);
    static final Color GRIS = new Color(0x9AA6C8);

    static final Color[] PALETA = { ROSA, CIAN, MENTA, new Color(0xA78BFA), new Color(0xFF8A5B) };

    static final String FUENTE = elegir("Fredoka", "Fredoka One", "Baloo 2", "Nunito", "Poppins",
            "Segoe UI", "SF Pro Rounded", "Helvetica Neue", "SansSerif");
    static final String MONO = elegir("JetBrains Mono", "Fira Code", "Cascadia Mono", "Consolas",
            "Menlo", "DejaVu Sans Mono", "Monospaced");

    static final String[] CODIGO = {
            "Nodo mezclar(Nodo[] listas) {",
            "  PriorityQueue<Nodo> heap = new PriorityQueue<>((a, b) -> a.valor - b.valor);",
            "  Nodo cabeza = new Nodo(0), cola = cabeza;",
            "  for (Nodo lista : listas)",
            "    if (lista != null) heap.add(lista);  // cabezas de cada lista",
            "  while (!heap.isEmpty()) {",
            "    Nodo actual = heap.poll();  // el menor global",
            "    cola.siguiente = actual;  cola = actual;",
            "    if (actual.siguiente != null)",
            "      heap.add(actual.siguiente);  // si existe",
            "  }",
            "  cola.siguiente = null;",
            "  return cabeza.siguiente;",
            "}"
    };

    static final String[] CODIGO_COMPARACION = {
            "Nodo mezclar(Lista[] listas) {",
            "  Nodo cabeza = new Nodo(0), cola = cabeza;",
            "  while (queden nodos) {",
            "    Nodo menor = null;",
            "    para cada lista:                         // comparar cabezas",
            "      si tiene nodos y (menor == null || cabeza.valor < menor.valor)",
            "        menor = cabeza;",
            "    cola.siguiente = menor; cola = menor;",
            "    avanzar en la lista del menor;",
            "  }",
            "  cola.siguiente = null;",
            "  return cabeza.siguiente;",
            "}"
    };

    // Ejemplos del menu: los de la tabla de casos borde de la presentacion y uno con intercambios
    static final String[][] EJEMPLOS = {
            { "Ejemplo de la presentaci\u00f3n", "[1,4,7],[2,5,8],[3,6,9]" },
            { "Con intercambios en el heap", "[5,9],[3,8],[1,7]" },
            { "Todas las listas vac\u00edas", "[],[]" },
            { "No hay listas (k = 0)", "" },
            { "Solo una lista", "[1,3,5]" },
            { "Una lista vac\u00eda entre otras", "[1,4],[],[2,3]" },
            { "Valores repetidos", "[1,3],[1,2]" },
            { "Valores negativos", "[-5,1],[-3,2]" },
            { "Listas de tama\u00f1os distintos", "[1],[2,3,4,5]" },
            { "Comparaci\u00f3n uno por uno", "[2,3,4,5],[1,4,10],[5,6,11]" }
    };

    // ---------------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------------
    static String elegir(String... familias) {
        Set<String> disponibles = new HashSet<String>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : familias) if (disponibles.contains(f)) return f;
        return familias[familias.length - 1];
    }

    static Color alfa(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mezcla(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static double suave(double t) {                  
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /** Lee "[1,4,7],[2,5,8]" (o "1,4,7 | 2,5,8"). Lanza IllegalArgumentException con un mensaje claro. */
    static int[][] parse(String texto) {
        String s = texto == null ? "" : texto.trim();
        if (s.isEmpty()) return new int[0][];
        List<String> grupos = new ArrayList<String>();
        if (s.indexOf('[') >= 0) {
            Matcher m = Pattern.compile("\\[([^\\]]*)\\]").matcher(s);
            while (m.find()) grupos.add(m.group(1));
        } else {
            for (String p : s.split("[|;/]")) grupos.add(p);
        }
        if (grupos.isEmpty())
            throw new IllegalArgumentException("Formato: [1,4,7],[2,5,8],[3,6,9]  (una lista vac\u00eda se escribe [])");
        if (grupos.size() > MAX_LISTAS)
            throw new IllegalArgumentException("M\u00e1ximo " + MAX_LISTAS + " listas (k).");
        int[][] datos = new int[grupos.size()][];
        int total = 0;
        for (int i = 0; i < datos.length; i++) {
            String g = grupos.get(i).replaceAll("^[,\\s]+|[,\\s]+$", "");
            if (g.isEmpty() || g.equals("-")) { datos[i] = new int[0]; continue; }
            String[] partes = g.split("[,\\s]+");
            if (partes.length > MAX_POR_LISTA)
                throw new IllegalArgumentException("L" + (i + 1) + ": m\u00e1ximo " + MAX_POR_LISTA + " nodos por lista.");
            datos[i] = new int[partes.length];
            for (int j = 0; j < partes.length; j++) {
                try {
                    datos[i][j] = Integer.parseInt(partes[j]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("\u00ab" + partes[j] + "\u00bb no es un n\u00famero entero.");
                }
                if (datos[i][j] < -99 || datos[i][j] > 999)
                    throw new IllegalArgumentException("Usa valores entre -99 y 999.");
                if (j > 0 && datos[i][j] < datos[i][j - 1])
                    throw new IllegalArgumentException("L" + (i + 1) + " no est\u00e1 ordenada de menor a mayor.");
            }
            total += partes.length;
        }
        if (total > MAX_TOTAL)
            throw new IllegalArgumentException("M\u00e1ximo " + MAX_TOTAL + " nodos en total (N).");
        return datos;
    }

    // =====================================================================
    //  MODELO: simula el algoritmo y guarda una "foto" por cada paso
    // =====================================================================

    /** Un nodo de las listas de entrada (cada uno es un circulo en pantalla). */
    static final class Item {
        final int id, valor, lista, pos;
        Item(int id, int valor, int lista, int pos) {
            this.id = id; this.valor = valor; this.lista = lista; this.pos = pos;
        }
    }

    /** Estado completo del algoritmo en un instante (un paso de la animacion). */
    static final class Snap {
        int[] salidos;      // por lista: cuantos nodos ya salieron de ella (hacia el heap)
        int[] heap;         // ids de los nodos en el orden del arreglo del heap
        int[] resultado;    // ids ya enlazados a la lista resultante
        int[] calientes;    // ids resaltados en este paso
        int[] codigo;       // lineas de codigo activas
        int actual = -1;    // id del nodo guardado en la variable "actual" (o -1)
        int maxHeap;        // tamano maximo que ha tenido el heap hasta este paso
        boolean cabeza;     // existe el nodo auxiliar "cabeza"
        boolean nullFinal;  // ya se ejecuto cola.siguiente = null
        boolean listo;      // ya se ejecuto return cabeza.siguiente
        String titulo, texto;
    }

    /** Ejecuta el algoritmo de verdad (con sift-up y sift-down como java.util.PriorityQueue). */
    static final class Modelo {
        final int k, total;
        final Item[][] listas;
        final Item[] items;
        final List<Snap> pasos = new ArrayList<Snap>();
        final boolean comparacion;

        private final int[] salidos;
        private final List<Integer> heap = new ArrayList<Integer>();
        private final List<Integer> res = new ArrayList<Integer>();
        private int actual = -1, maxHeap = 0;
        private boolean cabeza, nullFinal, listo;

        Modelo(int[][] datos) { this(datos, false); }

        Modelo(int[][] datos, boolean comparacion) {
            this.comparacion = comparacion;
            k = datos.length;
            int n = 0;
            for (int[] d : datos) n += d.length;
            total = n;
            listas = new Item[k][];
            items = new Item[n];
            int id = 0;
            for (int i = 0; i < k; i++) {
                listas[i] = new Item[datos[i].length];
                for (int j = 0; j < datos[i].length; j++) {
                    listas[i][j] = items[id] = new Item(id, datos[i][j], i, j);
                    id++;
                }
            }
            salidos = new int[k];
            if (comparacion) simularComparacion();
            else simular();
        }

        private int v(int id) { return items[id].valor; }

        private static int[] L(int... lineas) { return lineas; }

        private static int[] aArreglo(List<Integer> l) {
            int[] a = new int[l.size()];
            for (int i = 0; i < a.length; i++) a[i] = l.get(i);
            return a;
        }

        private void actualizarMax() { maxHeap = Math.max(maxHeap, heap.size()); }

        private void snap(String titulo, String texto, int[] codigo, int... calientes) {
            Snap s = new Snap();
            s.salidos = salidos.clone();
            s.heap = aArreglo(heap);
            s.resultado = aArreglo(res);
            s.actual = actual;
            s.cabeza = cabeza;
            s.nullFinal = nullFinal;
            s.listo = listo;
            s.maxHeap = maxHeap;
            s.calientes = calientes;
            s.codigo = codigo;
            s.titulo = titulo;
            s.texto = texto;
            pasos.add(s);
        }

        /** Nodo recien insertado en la posicion idx del heap y su padre (si lo tiene). */
        private int[] calInsercion(int idx) {
            int nodo = heap.get(idx);
            return idx == 0 ? new int[] { nodo } : new int[] { nodo, heap.get((idx - 1) >>> 1) };
        }

        private String notaInsercion(int idx) {
            if (idx == 0) return "Es el \u00fanico nodo del heap: queda en la ra\u00edz.";
            int pv = v(heap.get((idx - 1) >>> 1)), nv = v(heap.get(idx));
            return nv >= pv
                    ? "Entra como \u00faltimo nodo del heap; su padre (" + pv + ") no es mayor, as\u00ed que se queda ah\u00ed."
                    : "Entra como \u00faltimo nodo del heap; su padre (" + pv + ") es mayor, as\u00ed que debe subir.";
        }

        private int hijoMenor(int idx) {
            int h = 2 * idx + 1;
            if (h + 1 < heap.size() && v(heap.get(h)) > v(heap.get(h + 1))) h++;
            return h;
        }

        private boolean necesitaBajar(int idx) {
            if (2 * idx + 1 >= heap.size()) return false;
            return v(heap.get(idx)) > v(heap.get(hijoMenor(idx)));
        }

        /** sift-up: el nodo en idx sube mientras sea menor que su padre (un paso por intercambio). */
        private void subir(int idx, int[] codigo) {
            int nodo = heap.get(idx);
            while (idx > 0) {
                int p = (idx - 1) >>> 1;
                int padre = heap.get(p);
                if (v(nodo) >= v(padre)) break;
                heap.set(idx, padre);
                heap.set(p, nodo);
                idx = p;
                String t = v(nodo) + " es menor que su padre (" + v(padre) + "): intercambian y "
                        + v(nodo) + " sube un nivel.";
                if (idx == 0) {
                    t += " Lleg\u00f3 a la ra\u00edz: ya es el menor del heap.";
                } else {
                    int np = v(heap.get((idx - 1) >>> 1));
                    t += v(nodo) >= np
                            ? " Su nuevo padre (" + np + ") no es mayor: se detiene."
                            : " Su nuevo padre (" + np + ") sigue siendo mayor: contin\u00faa subiendo.";
                }
                snap("Subir en el heap", t, codigo, nodo, padre);
            }
        }

        /** sift-down: el nodo en idx baja mientras tenga un hijo menor (un paso por intercambio). */
        private void bajar(int idx, int[] codigo) {
            if (idx >= heap.size()) return;                   // el heap quedo vacio: nada que bajar
            int nodo = heap.get(idx);
            while (necesitaBajar(idx)) {
                int hijo = hijoMenor(idx);
                int h = heap.get(hijo);
                heap.set(idx, h);
                heap.set(hijo, nodo);
                idx = hijo;
                String t = v(nodo) + " es mayor que su hijo menor (" + v(h) + "): intercambian y "
                        + v(nodo) + " baja un nivel.";
                if (2 * idx + 1 >= heap.size()) {
                    t += " Ya no tiene hijos: se detiene.";
                } else {
                    int m = v(heap.get(hijoMenor(idx)));
                    t += v(nodo) <= m
                            ? " Ya no es mayor que sus hijos: se detiene."
                            : " Su hijo menor (" + m + ") todav\u00eda es menor: contin\u00faa bajando.";
                }
                snap("Bajar en el heap", t, codigo, nodo, h);
            }
        }

        private void simularComparacion() {
            snap("Inicio", "Comparamos directamente las cabezas de las listas. No se crea ni se utiliza un heap.", L(0));
            cabeza = true;
            snap("Preparación", "Creamos la lista resultante con un nodo auxiliar cabeza. En cada ronda revisaremos las listas y escogeremos la cabeza con menor valor.", L(1, 2));

            while (res.size() < total) {
                int menor = -1;
                for (int i = 0; i < k; i++) {
                    if (salidos[i] >= listas[i].length) {
                        snap("Revisar L" + (i + 1), "La lista L" + (i + 1) + " ya no tiene nodos disponibles, por lo que se omite.", L(4, 5));
                        continue;
                    }
                    int id = listas[i][salidos[i]].id;
                    if (menor == -1) {
                        menor = id;
                        snap("Primer candidato", "La cabeza de L" + (i + 1) + " (" + v(id) + ") es el primer candidato a mínimo.", L(3, 4, 5), id);
                    } else {
                        int anterior = menor;
                        if (v(id) < v(menor)) menor = id;
                        snap("Comparar cabezas", "Comparamos " + v(id) + " de L" + (i + 1) + " con " + v(anterior) + ". "
                                        + (menor == id ? v(id) + " es menor y pasa a ser el candidato." : v(anterior) + " sigue siendo el menor candidato."),
                                L(4, 5, 6), id, menor);
                    }
                }
                if (menor == -1) break;
                actual = menor;
                Item elegido = items[menor];
                salidos[elegido.lista]++;
                snap("Seleccionar el menor", "El menor de las cabezas revisadas es " + v(menor) + ". Se añade al final del resultado y avanzamos en L" + (elegido.lista + 1) + ".", L(6, 7, 8), menor);
                actual = -1;
                res.add(menor);
                snap("Avanzar en la lista", v(menor) + " ya forma parte del resultado. Solo avanzamos en L" + (elegido.lista + 1) + "; las demás listas permanecen igual.", L(7, 8), menor);
            }
            snap("Fin del ciclo", "Ya no quedan nodos por revisar. El resultado contiene todos los elementos de las listas originales.", L(9));
            nullFinal = true;
            snap("Cerrar la lista", "La última referencia apunta a null para marcar el final de la lista resultante.", L(10));
            listo = true;
            cabeza = false;
            snap("Resultado", "Lista ordenada: " + total + " nodos. En cada extracción revisamos hasta k cabezas, por lo que el costo total es O(N·k).", L(11));
        }

        private void simular() {
            String t0;
            if (k == 0) {
                t0 = "No hay listas (k = 0), as\u00ed que no hay nada que mezclar. Veamos c\u00f3mo lo resuelve el algoritmo.";
            } else if (total == 0) {
                t0 = (k == 1 ? "La lista est\u00e1 vac\u00eda (null)" : "Las " + k + " listas est\u00e1n vac\u00edas (null)")
                        + ", as\u00ed que no hay nada que mezclar. Veamos c\u00f3mo lo resuelve el algoritmo.";
            } else {
                t0 = "Tenemos " + k + (k == 1 ? " lista ordenada" : " listas ordenadas") + " con " + total
                        + (total == 1 ? " nodo" : " nodos") + " en total. Como est\u00e1n ordenadas, cada nodo cabeza "
                        + "(recuadro amarillo) es el menor de su lista. Idea voraz: el siguiente n\u00famero del "
                        + "resultado siempre es el menor de las cabezas actuales.";
            }
            snap("Inicio", t0, L(0));

            cabeza = true;
            snap("Preparaci\u00f3n", "Se crea un min-heap vac\u00edo y un nodo auxiliar \u00abcabeza\u00bb para armar el resultado. "
                    + "\u00abcola\u00bb apunta al \u00faltimo nodo enlazado (por ahora, \u00abcabeza\u00bb).", L(1, 2));

            // 1) las cabezas de cada lista entran al heap
            for (int i = 0; i < k; i++) {
                if (listas[i].length == 0) {
                    snap("heap.add(lista)", "L" + (i + 1) + " es null (lista vac\u00eda): se omite y no entra al heap.", L(3, 4));
                    continue;
                }
                Item n = listas[i][0];
                salidos[i] = 1;
                heap.add(n.id);
                actualizarMax();
                int idx = heap.size() - 1;
                snap("heap.add(lista)", "La cabeza de L" + (i + 1) + " (" + n.valor + ") pasa al min-heap. "
                        + notaInsercion(idx), L(3, 4), calInsercion(idx));
                subir(idx, L(3, 4));
            }

            // 2) ciclo voraz: sacar el menor, enlazarlo y reponer con su siguiente
            while (!heap.isEmpty()) {
                int raiz = heap.get(0);
                int ultimo = heap.remove(heap.size() - 1);
                String t = "poll(): sale " + v(raiz) + " del heap, el menor de todo lo que queda sin mezclar.";
                int[] cal;
                if (!heap.isEmpty()) {
                    heap.set(0, ultimo);
                    t += " El \u00faltimo nodo del heap (" + v(ultimo) + ") pasa a la ra\u00edz"
                            + (necesitaBajar(0) ? " y, como tiene un hijo menor, debe bajar."
                            : " y ya cumple la propiedad min-heap.");
                    cal = new int[] { raiz, ultimo };
                } else {
                    cal = new int[] { raiz };
                }
                actual = raiz;
                snap("poll()", t, L(6), cal);
                bajar(0, L(6));

                int a = actual;
                actual = -1;
                res.add(a);
                Item ia = items[a];
                Item sig = ia.pos + 1 < listas[ia.lista].length ? listas[ia.lista][ia.pos + 1] : null;
                if (sig != null) {
                    salidos[ia.lista]++;
                    heap.add(sig.id);
                    actualizarMax();
                    int idx = heap.size() - 1;
                    int[] ci = calInsercion(idx);
                    int[] cal2 = new int[ci.length + 1];
                    cal2[0] = a;
                    System.arraycopy(ci, 0, cal2, 1, ci.length);
                    snap("Enlazar y reponer", v(a) + " se enlaza al final del resultado y \u00abcola\u00bb avanza hasta \u00e9l. "
                            + "Su siguiente en L" + (ia.lista + 1) + " (" + sig.valor + ") sale de su lista y entra al heap. "
                            + notaInsercion(idx), L(7, 8, 9), cal2);
                    subir(idx, L(7, 8, 9));
                } else {
                    snap("Enlazar", v(a) + " se enlaza al final del resultado y \u00abcola\u00bb avanza hasta \u00e9l. "
                            + "Esa lista ya qued\u00f3 vac\u00eda (actual.siguiente es null): no entra nada al heap.", L(7, 8), a);
                }
            }

            // 3) cierre
            snap("Fin del ciclo", total > 0
                    ? "El heap qued\u00f3 vac\u00edo y las listas tambi\u00e9n: ya no queda nada por mezclar."
                    : "El heap est\u00e1 vac\u00edo, as\u00ed que el ciclo while ni siquiera empieza.", L(5, 10));
            nullFinal = true;
            snap("Cerrar la lista", "cola.siguiente = null: la lista resultante termina en null.", L(11));
            listo = true;
            cabeza = false;
            snap("Resultado", total > 0
                    ? "return cabeza.siguiente: se devuelve la lista ordenada con los " + total
                    + " nodos, sin el nodo auxiliar. El heap nunca pas\u00f3 de " + maxHeap
                    + (maxHeap == 1 ? " nodo" : " nodos") + ", por eso cada operaci\u00f3n cuesta O(log\u00a0k) "
                    + "y el total es O(N\u00a0log\u00a0k)."
                    : "return cabeza.siguiente: como no hubo nodos, se devuelve null.", L(12));
        }
    }

    // =====================================================================
    //  ANIMACION
    // =====================================================================

    /** Interpola posicion, tamano, transparencia y brillo entre dos pasos. */
    static final class Mover {
        boolean curvo;                                        // true: se mueve en arco (los cruces no se tapan)
        double x, y, r, a, g;
        double x0, y0, r0, a0, g0;
        double x1, y1, r1, a1, g1;

        void ir(double x, double y, double r, double a, double g) {
            x0 = this.x; y0 = this.y; r0 = this.r; a0 = this.a; g0 = this.g;
            x1 = x; y1 = y; r1 = r; a1 = a; g1 = g;
        }

        void mezclar(double e) {
            x = x0 + (x1 - x0) * e;
            y = y0 + (y1 - y0) * e;
            if (curvo) {
                double dx = x1 - x0, dy = y1 - y0, d = Math.hypot(dx, dy);
                if (d > 1e-6) {
                    double desvio = Math.min(40, 0.36 * d) * Math.sin(Math.PI * e);
                    x += -dy / d * desvio;
                    y += dx / d * desvio;
                }
            }
            r = r0 + (r1 - r0) * e;
            a = a0 + (a1 - a0) * e;
            g = g0 + (g1 - g0) * e;
        }

        void llegar() { x = x1; y = y1; r = r1; a = a1; g = g1; }
    }

    @SuppressWarnings("serial")
    static final class AnimPanel extends JPanel {
        // geometria del lienzo logico (1280 x 720)
        static final int IN_X = 24, IN_W = 470, TOP_Y = 70, TOP_H = 334;
        static final int HP_X = 510, HP_W = 330, RP_X = 856, RP_W = 400;
        static final int BOT_Y = 416, BOT_H = 292, CODE_X = 510, CODE_W = 746;
        static final int R_IN = 22, R_HEAP = 24, R_RES = 17;
        static final double RES_X0 = 968, RES_DX = 58, RES_Y0 = 158, RES_DY = 52, ACT_X = 890;
        static final double[] VELOCIDADES = { 0.5, 0.75, 1, 1.5, 2, 3 };

        Modelo modelo;
        List<Snap> pasos;
        Mover[] mv = new Mover[0];
        final Mover cabezaMv = new Mover(), colaMv = new Mover(), finMv = new Mover(),
                nullMv = new Mover(), cajaMv = new Mover();
        final List<Mover> todos = new ArrayList<Mover>();
        final double[] lineaA = new double[CODIGO.length];
        final Timer reloj;
        final Font base = new Font(FUENTE, Font.PLAIN, 12);
        int cur, prevPaso, k, maxLen, capacidad, velIdx = 2;
        boolean animando, reproduciendo;
        double t = 1, durMs = 700, espera, tiempo, tPaso = 1000;
        long ultimo = System.nanoTime();
        Runnable alCambiar;

        AnimPanel() {
            setPreferredSize(new Dimension(960, 540));
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
            });
            reloj = new Timer(16, e -> {
                long ahora = System.nanoTime();
                double dt = Math.min(80, (ahora - ultimo) / 1e6);
                ultimo = ahora;
                avanzar(dt);
            });
        }

        @Override public void addNotify() {
            super.addNotify();
            ultimo = System.nanoTime();
            reloj.start();
        }

        @Override public void removeNotify() {
            reloj.stop();
            super.removeNotify();
        }

        // ---------------- control ----------------
        void avisar() { if (alCambiar != null) alCambiar.run(); }

        void cargar(int[][] datos) { cargar(datos, false); }

        void cargar(int[][] datos, boolean comparacion) {
            modelo = new Modelo(datos, comparacion);
            pasos = modelo.pasos;
            k = modelo.k;
            maxLen = 0;
            capacidad = 0;
            for (Item[] l : modelo.listas) {
                maxLen = Math.max(maxLen, l.length);
                if (l.length > 0) capacidad++;
            }
            mv = new Mover[modelo.total];
            todos.clear();
            for (int i = 0; i < mv.length; i++) { mv[i] = new Mover(); mv[i].curvo = true; todos.add(mv[i]); }
            todos.add(cabezaMv); todos.add(colaMv); todos.add(finMv); todos.add(nullMv); todos.add(cajaMv);
            cur = 0; prevPaso = 0;
            animando = false; reproduciendo = false; t = 1; tPaso = 1000;
            aplicar(0);
            for (Mover m : todos) m.llegar();
            Arrays.fill(lineaA, 0);
            for (int c : pasos.get(0).codigo) lineaA[c] = 1;
            avisar();
            repaint();
        }

        void irA(int paso, boolean animar) {
            if (paso < 0 || paso >= pasos.size()) return;
            prevPaso = cur;
            cur = paso;
            aplicar(paso);
            tPaso = 0;
            if (animar) {
                t = 0;
                animando = true;
                durMs = Math.max(170, 720 / VELOCIDADES[velIdx]);
                int largo = pasos.get(paso).texto.length();
                espera = Math.max(900, (700 + 24.0 * largo) / VELOCIDADES[velIdx]);   // tiempo para leer
            } else {
                for (Mover m : todos) m.llegar();
                animando = false;
                t = 1;
            }
        }

        void siguiente() { if (cur < pasos.size() - 1) irA(cur + 1, true); }

        void anterior() { if (cur > 0) irA(cur - 1, true); }

        void reiniciar() {
            reproduciendo = false;
            irA(0, true);
            avisar();
        }

        void alternar() {
            if (reproduciendo) {
                reproduciendo = false;
            } else {
                if (cur >= pasos.size() - 1) irA(0, true);
                reproduciendo = true;
                espera = animando ? 0 : 350;
            }
            avisar();
        }

        void cambiarVelocidad(int d) {
            velIdx = Math.max(0, Math.min(VELOCIDADES.length - 1, velIdx + d));
            avisar();
        }

        /** Avanza el reloj de la animacion dt milisegundos. */
        void avanzar(double dt) {
            tiempo += dt;
            tPaso += dt;
            if (animando) {
                t += dt / durMs;
                if (t >= 1) { t = 1; animando = false; }
                double e = suave(t);
                for (Mover m : todos) m.mezclar(e);
            }
            if (reproduciendo && !animando) {
                espera -= dt;
                if (espera <= 0) {
                    if (cur < pasos.size() - 1) {
                        siguiente();
                    } else {
                        reproduciendo = false;
                        avisar();
                    }
                }
            }
            double[] objetivo = new double[CODIGO.length];
            for (int c : pasos.get(cur).codigo) objetivo[c] = 1;
            double paso = Math.min(1, dt / 110.0);
            for (int i = 0; i < lineaA.length; i++) lineaA[i] += (objetivo[i] - lineaA[i]) * paso;
            repaint();
        }

        // ---------------- geometria ----------------
        Point2D.Double posEntrada(int lista, int j) {
            double areaTop = TOP_Y + 44, areaH = TOP_H - 56;
            double filaH = Math.min(66, areaH / Math.max(1, k));
            double y = areaTop + (areaH - filaH * k) / 2 + filaH * (lista + 0.5);
            double dx = maxLen > 1 ? Math.min(86, 270.0 / (maxLen - 1)) : 86;
            return new Point2D.Double(IN_X + 88 + dx * j, y);
        }

        Point2D.Double posNull(int lista) {
            int len = modelo.listas[lista].length;
            Point2D.Double p = posEntrada(lista, Math.max(0, len - 1));
            return new Point2D.Double(len == 0 ? p.x : p.x + 70, p.y);
        }

        Point2D.Double posHeap(int i) {
            int nivel = 31 - Integer.numberOfLeadingZeros(i + 1);
            int j = i - ((1 << nivel) - 1);
            int cuenta = 1 << nivel;
            double sep = 78.0 * (1 << (2 - Math.min(2, nivel)));
            double cx = HP_X + HP_W / 2.0;
            return new Point2D.Double(cx + (j - (cuenta - 1) / 2.0) * sep, TOP_Y + 86 + 76.0 * nivel);
        }

        static double slotX(int p) { return RES_X0 + (p % 5) * RES_DX; }

        static double slotY(int p) { return RES_Y0 + (p / 5) * RES_DY; }

        /** Calcula hacia donde debe ir cada elemento en el paso dado. */
        void aplicar(int paso) {
            Snap s = pasos.get(paso);
            int n = modelo.total;
            int[] donde = new int[n], idx = new int[n];       // donde: 0 lista, 1 heap, 2 actual, 3 resultado
            for (int i = 0; i < k; i++)
                for (int j = s.salidos[i]; j < modelo.listas[i].length; j++) {
                    int id = modelo.listas[i][j].id;
                    donde[id] = 0;
                    idx[id] = j;
                }
            for (int h = 0; h < s.heap.length; h++) { donde[s.heap[h]] = 1; idx[s.heap[h]] = h; }
            if (s.actual >= 0) donde[s.actual] = 2;
            for (int r = 0; r < s.resultado.length; r++) { donde[s.resultado[r]] = 3; idx[s.resultado[r]] = r; }
            boolean[] cal = new boolean[n];
            for (int id : s.calientes) cal[id] = true;
            int off = s.listo ? 0 : 1;                        // la "cabeza" ocupa la primera ranura
            for (Item it : modelo.items) {
                double x, y, r;
                int d = donde[it.id];
                if (d == 0) {
                    Point2D.Double p = posEntrada(it.lista, idx[it.id]);
                    x = p.x; y = p.y; r = R_IN;
                } else if (d == 1) {
                    Point2D.Double p = posHeap(idx[it.id]);
                    x = p.x; y = p.y; r = R_HEAP;
                } else if (d == 2) {
                    x = ACT_X; y = RES_Y0; r = R_IN;
                } else {
                    x = slotX(idx[it.id] + off); y = slotY(idx[it.id] + off); r = R_RES;
                }
                mv[it.id].ir(x, y, r, 1, (cal[it.id] || (s.listo && d == 3)) ? 1 : 0);
            }
            cabezaMv.ir(slotX(0), slotY(0), R_RES, s.cabeza ? 1 : 0, 0);
            int cola = s.resultado.length;                    // ranura del ultimo nodo (0 = cabeza)
            colaMv.ir(slotX(cola), slotY(cola) + R_RES + 7, 0, s.cabeza ? 1 : 0, 0);
            int nSlot = off + s.resultado.length;
            nullMv.ir(slotX(nSlot), slotY(nSlot), 0, s.nullFinal ? 1 : 0, 0);
            double fx, fy;
            if (s.nullFinal) {
                fx = slotX(nSlot) + 31; fy = slotY(nSlot);
            } else if (s.cabeza || s.resultado.length > 0) {
                int u = off + s.resultado.length - 1;
                fx = slotX(u) + R_RES + 12; fy = slotY(u);
            } else {
                fx = slotX(0) - 6; fy = slotY(0);
            }
            finMv.ir(fx, fy, 0, 1, 0);
            cajaMv.ir(0, 0, 0, (paso == 0 && modelo.total > 0) ? 1 : 0, 0);
        }

        // ---------------- dibujo ----------------
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int pw = getWidth(), ph = getHeight();
            g.setColor(FONDO);
            g.fillRect(0, 0, pw, ph);
            if (modelo != null && pw > 0 && ph > 0) {
                double sc = Math.min(pw / (double) W, ph / (double) H);
                g.translate((pw - W * sc) / 2, (ph - H * sc) / 2);
                g.scale(sc, sc);
                escena(g);
            }
            g.dispose();
        }

        void escena(Graphics2D g) {
            Snap s = pasos.get(cur);
            encabezado(g);
            panel(g, IN_X, TOP_Y, IN_W, TOP_H, "Listas de entrada", ROSA,
                    k == 0 ? null : "k = " + k + ", N = " + modelo.total);
            panel(g, HP_X, TOP_Y, HP_W, TOP_H,
                    modelo.comparacion ? "Comparación directa" : "Min-heap", CIAN,
                    modelo.comparacion ? "sin estructura auxiliar" : "tamaño " + s.heap.length);
            panel(g, RP_X, TOP_Y, RP_W, TOP_H, "Lista resultante", MENTA,
                    s.resultado.length + " de " + modelo.total + " nodos");
            panel(g, IN_X, BOT_Y, IN_W, BOT_H, "Qu\u00e9 pasa en este paso", AMBAR, null);
            panel(g, CODE_X, BOT_Y, CODE_W, BOT_H, "C\u00f3digo en Java", TENUE, null);
            entrada(g, s);
            if (modelo.comparacion) comparacionVisual(g, s);
            else arbol(g, s);
            resultado(g, s);
            nodos(g, s);
            explicacion(g, s);
            codigo(g, s);
        }

        void encabezado(Graphics2D g) {
            g.setFont(base.deriveFont(Font.BOLD, 26f));
            g.setColor(ROSA);
            g.drawString("Algoritmo voraz para mezclar k listas ordenadas", 28, 36);
            g.setFont(base.deriveFont(Font.PLAIN, 13.5f));
            g.setColor(TENUE);
            g.drawString(modelo.comparacion
                    ? "Sin heap: se comparan una por una las cabezas actuales y se avanza en la lista elegida."
                    : "Con un Min-heap: en cada paso se toma el menor de las cabezas actuales, sin volver atrás.", 28, 57);
            double p = pasos.size() > 1
                    ? (animando ? prevPaso + (cur - prevPaso) * suave(t) : cur) / (pasos.size() - 1.0) : 1;
            g.setFont(base.deriveFont(Font.BOLD, 15f));
            g.setColor(TEXTO);
            derecha(g, "Paso " + (cur + 1) + " de " + pasos.size(), W - 28, 30);
            double x0 = W - 28 - 220;
            g.setColor(new Color(255, 255, 255, 30));
            g.fill(new RoundRectangle2D.Double(x0, 40, 220, 7, 7, 7));
            g.setColor(CIAN);
            g.fill(new RoundRectangle2D.Double(x0, 40, Math.max(7, 220 * p), 7, 7, 7));
        }

        /** Tarjeta oscura con sombra dura desplazada, como las tarjetas de la presentacion. */
        void panel(Graphics2D g, int x, int y, int w, int h, String titulo, Color colorTitulo, String derecha) {
            g.setColor(new Color(26, 46, 134));
            g.fill(new RoundRectangle2D.Double(x - 7, y + 7, w, h, 22, 22));
            RoundRectangle2D.Double tarjeta = new RoundRectangle2D.Double(x, y, w, h, 22, 22);
            g.setColor(new Color(8, 15, 45));
            g.fill(tarjeta);
            g.setColor(new Color(70, 112, 235));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(tarjeta);
            g.setFont(base.deriveFont(Font.BOLD, 17f));
            g.setColor(colorTitulo);
            g.drawString(titulo, x + 20, y + 31);
            if (derecha != null) {
                g.setFont(base.deriveFont(Font.PLAIN, 13f));
                g.setColor(TENUE);
                derecha(g, derecha, x + w - 20, y + 30);
            }
        }

        void entrada(Graphics2D g, Snap s) {
            if (k == 0) {
                g.setFont(base.deriveFont(Font.PLAIN, 16f));
                g.setColor(TENUE);
                centrar(g, "No hay listas de entrada", IN_X + IN_W / 2.0, TOP_Y + TOP_H / 2.0);
                return;
            }
            if (cajaMv.a > 0.01) {                            // recuadro amarillo sobre las cabezas (paso 0)
                Point2D.Double a = posEntrada(0, 0), b = posEntrada(k - 1, 0);
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, cajaMv.a)));
                g.setColor(AMBAR);
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Double(a.x - R_IN - 11, a.y - R_IN - 11, 2 * (R_IN + 11),
                        (b.y - a.y) + 2 * (R_IN + 11), 18, 18));
                g.setComposite(old);
            }
            for (int i = 0; i < k; i++) {
                Item[] lista = modelo.listas[i];
                int len = lista.length;
                Point2D.Double p0 = posEntrada(i, 0);
                g.setFont(base.deriveFont(Font.BOLD, 20f));
                g.setColor(PALETA[i]);
                g.drawString("L" + (i + 1) + ":", IN_X + 20, (float) (p0.y + 7));
                for (int j = 0; j < s.salidos[i]; j++) {      // huecos de los nodos que ya salieron
                    Point2D.Double p = posEntrada(i, j);
                    fantasma(g, p.x, p.y, R_IN, new Color(140, 165, 230, 85));
                }
                Point2D.Double pn = posNull(i);
                for (int j = s.salidos[i]; j < len; j++) {
                    Mover a = mv[lista[j].id];
                    if (j + 1 < len) {
                        Mover b = mv[lista[j + 1].id];
                        flecha(g, a.x, a.y, a.r, b.x, b.y, b.r, PALETA[i]);
                    } else {
                        flecha(g, a.x, a.y, a.r, pn.x, pn.y, 19, PALETA[i]);
                    }
                }
                g.setFont(base.deriveFont(Font.BOLD, 16f));
                if (len == 0) {
                    g.setColor(alfa(TENUE, 190));
                    centrar(g, "null", pn.x, pn.y);
                } else if (s.salidos[i] < len) {
                    g.setColor(PALETA[i]);
                    centrar(g, "null", pn.x, pn.y);
                } else {
                    g.setFont(base.deriveFont(Font.ITALIC, 13f));
                    g.setColor(alfa(TENUE, 170));
                    centrar(g, "vac\u00eda", pn.x, pn.y);
                }
            }
        }

        void comparacionVisual(Graphics2D g, Snap s) {
            double cx = HP_X + HP_W / 2.0;
            g.setFont(base.deriveFont(Font.BOLD, 19f));
            g.setColor(CIAN);
            centrar(g, "Comparar las cabezas", cx, TOP_Y + 85);
            g.setFont(base.deriveFont(Font.PLAIN, 15f));
            g.setColor(TEXTO);
            centrar(g, "1. Revisar cada lista", cx, TOP_Y + 125);
            centrar(g, "2. Conservar el menor", cx, TOP_Y + 155);
            centrar(g, "3. Avanzar solo esa lista", cx, TOP_Y + 185);
            g.setColor(MENTA);
            g.setFont(base.deriveFont(Font.BOLD, 17f));
            centrar(g, "Sin Min-heap", cx, TOP_Y + 235);
            g.setColor(TENUE);
            g.setFont(base.deriveFont(Font.PLAIN, 13f));
            centrar(g, "Costo: O(N · k)", cx, TOP_Y + 263);
        }

        void arbol(Graphics2D g, Snap s) {
            // esqueleto del arbol con las ranuras disponibles (como los cuadros vacios de la presentacion)
            g.setStroke(new BasicStroke(2f));
            for (int i = 1; i < capacidad; i++) {
                Point2D.Double p = posHeap(i), q = posHeap((i - 1) / 2);
                segmento(g, q.x, q.y, p.x, p.y, R_HEAP + 3, new Color(120, 160, 230, 45));
            }
            for (int i = s.heap.length; i < capacidad; i++) {
                Point2D.Double p = posHeap(i);
                fantasma(g, p.x, p.y, R_HEAP, new Color(140, 165, 230, 85));
            }
            // aristas reales: siguen a los nodos mientras se mueven
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 190, 255, 160));
            for (int h = 1; h < s.heap.length; h++) {
                Mover a = mv[s.heap[(h - 1) / 2]], b = mv[s.heap[h]];
                g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
            }
            // el mismo heap visto como arreglo
            if (capacidad > 0) {
                Snap a = (animando && suave(t) < 0.5) ? pasos.get(prevPaso) : s;
                int cw = 42, ch = 32, sep = 4;
                double total = 64 + capacidad * (cw + sep) - sep;
                double x0 = HP_X + (HP_W - total) / 2.0 + 64, y0 = TOP_Y + TOP_H - 58;
                g.setFont(base.deriveFont(Font.PLAIN, 12.5f));
                g.setColor(TENUE);
                g.drawString("arreglo", (float) (x0 - 64), (float) (y0 + 21));
                for (int c = 0; c < capacidad; c++) {
                    double x = x0 + c * (cw + sep);
                    RoundRectangle2D.Double celda = new RoundRectangle2D.Double(x, y0, cw, ch, 10, 10);
                    boolean lleno = c < a.heap.length;
                    Color col = lleno ? PALETA[modelo.items[a.heap[c]].lista] : new Color(140, 165, 230);
                    g.setColor(lleno ? alfa(col, 40) : new Color(255, 255, 255, 8));
                    g.fill(celda);
                    g.setColor(lleno ? col : new Color(140, 165, 230, 90));
                    g.setStroke(new BasicStroke(1.8f));
                    g.draw(celda);
                    if (lleno) {
                        g.setFont(base.deriveFont(Font.BOLD, 15f));
                        g.setColor(Color.WHITE);
                        centrar(g, String.valueOf(modelo.items[a.heap[c]].valor), x + cw / 2.0, y0 + ch / 2.0);
                    }
                    g.setFont(base.deriveFont(Font.PLAIN, 11f));
                    g.setColor(TENUE);
                    centrar(g, String.valueOf(c), x + cw / 2.0, y0 + ch + 12);
                }
            }
        }

        void resultado(Graphics2D g, Snap s) {
            // ranura "actual": aqui espera el nodo que sale del heap antes de enlazarse
            fantasma(g, ACT_X, RES_Y0, R_IN, new Color(140, 165, 230, 110));
            g.setFont(base.deriveFont(Font.PLAIN, 12f));
            g.setColor(TENUE);
            centrar(g, "actual", ACT_X, RES_Y0 - R_IN - 12);
            flecha(g, ACT_X, RES_Y0, R_IN, RES_X0 - 28, RES_Y0, 0, TENUE);

            corchete(g, RES_X0 - 26, RES_Y0, true, 1);
            if (finMv.a > 0.01) corchete(g, finMv.x, finMv.y, false, finMv.a);

            List<Mover> seq = new ArrayList<Mover>();
            List<Color> cols = new ArrayList<Color>();
            if (s.cabeza) { seq.add(cabezaMv); cols.add(GRIS); }
            for (int id : s.resultado) { seq.add(mv[id]); cols.add(PALETA[modelo.items[id].lista]); }
            if (s.nullFinal) { seq.add(nullMv); cols.add(TENUE); }
            Composite old = g.getComposite();
            for (int q = 0; q + 1 < seq.size(); q++) {        // enlaces: cada nodo apunta al siguiente
                Mover a = seq.get(q), b = seq.get(q + 1);
                boolean salto = (q + 1) % 5 == 0;              // la lista continua en la fila de abajo
                double rb = b == nullMv ? 19 : R_RES;
                g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, Math.min(a.a, b.a)))));
                if (salto) flecha(g, b.x - 46, b.y, 0, b.x, b.y, rb, cols.get(q));
                else flecha(g, a.x, a.y, R_RES, b.x, b.y, rb, cols.get(q));
            }
            g.setComposite(old);
            if (nullMv.a > 0.01) {
                g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, nullMv.a)));
                g.setFont(base.deriveFont(Font.BOLD, 15f));
                g.setColor(TENUE);
                centrar(g, "null", nullMv.x, nullMv.y);
                g.setComposite(old);
            }
            if (colaMv.a > 0.01) puntero(g, colaMv.x, colaMv.y, "cola", colaMv.a);
        }

        void nodos(Graphics2D g, Snap s) {
            double pulso = 0.5 + 0.5 * Math.sin(tiempo / 190.0);
            Color brillo = s.listo ? MENTA : AMBAR;
            if (cabezaMv.a > 0.01) {                          // nodo auxiliar "cabeza"
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, cabezaMv.a)));
                fantasma(g, cabezaMv.x, cabezaMv.y, R_RES, new Color(160, 175, 215, 210));
                g.setFont(base.deriveFont(Font.PLAIN, 11.5f));
                g.setColor(GRIS);
                centrar(g, "cabeza", cabezaMv.x, cabezaMv.y - R_RES - 13);
                g.setComposite(old);
            }
            for (int pasada = 0; pasada < 2; pasada++) {      // primero los tranquilos, encima los resaltados
                for (Item it : modelo.items) {
                    Mover m = mv[it.id];
                    if ((m.g > 0.04) != (pasada == 1)) continue;
                    nodo(g, m.x, m.y, m.r, PALETA[it.lista], String.valueOf(it.valor), m.a, m.g, brillo, pulso);
                }
            }
        }

        /** Circulo con aro de color, numero blanco y, si esta resaltado, un halo. */
        void nodo(Graphics2D g, double x, double y, double r, Color c, String txt,
                  double alpha, double glow, Color brillo, double pulso) {
            if (alpha <= 0.01 || r <= 0) return;
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, alpha)));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) (r * 1.9), new float[] { 0.45f, 1f },
                    new Color[] { alfa(c, 55), alfa(c, 0) }));
            g.fill(new Ellipse2D.Double(x - r * 1.9, y - r * 1.9, r * 3.8, r * 3.8));
            if (glow > 0.01) {
                int ga = (int) (glow * (70 + 35 * pulso));
                g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) (r * 2.6), new float[] { 0.3f, 1f },
                        new Color[] { alfa(brillo, ga), alfa(brillo, 0) }));
                g.fill(new Ellipse2D.Double(x - r * 2.6, y - r * 2.6, r * 5.2, r * 5.2));
            }
            g.setColor(new Color(9, 17, 48));
            g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            g.setColor(alfa(c, 38));
            g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            double gro = Math.max(2.2, r * 0.19);
            g.setStroke(new BasicStroke((float) gro));
            g.setColor(c);
            double rr = r - gro / 2;
            g.draw(new Ellipse2D.Double(x - rr, y - rr, 2 * rr, 2 * rr));
            if (glow > 0.01 && brillo.equals(AMBAR)) {
                g.setStroke(new BasicStroke(2.2f));
                g.setColor(alfa(brillo, (int) (glow * (200 + 55 * pulso))));
                double rh = r + 5 + 1.5 * pulso;
                g.draw(new Ellipse2D.Double(x - rh, y - rh, 2 * rh, 2 * rh));
            }
            g.setFont(base.deriveFont(Font.BOLD, (float) (r * 0.98)));
            g.setColor(Color.WHITE);
            centrar(g, txt, x, y);
            g.setComposite(old);
        }

        void explicacion(Graphics2D g, Snap s) {
            int x = IN_X + 20, y = BOT_Y + 68;
            g.setFont(base.deriveFont(Font.BOLD, 22f));
            g.setColor(CIAN);
            g.drawString(s.titulo, x, y);
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, tPaso / 260.0)));
            g.setFont(base.deriveFont(Font.PLAIN, 15.5f));
            g.setColor(TEXTO);
            envolver(g, s.texto, x, y + 34, IN_W - 40, 22);
            g.setComposite(old);
            // contadores
            double cy = BOT_Y + BOT_H - 46;
            g.setFont(base.deriveFont(Font.BOLD, 12.5f));
            String c1 = "Mezclados " + s.resultado.length + " de " + modelo.total;
            String c2 = "Heap " + s.heap.length + ", m\u00e1x. " + s.maxHeap;
            FontMetrics fm = g.getFontMetrics();
            double w1 = fm.stringWidth(c1) + 28, w2 = fm.stringWidth(c2) + 28;
            chip(g, x, cy, w1, c1, MENTA);
            chip(g, x + w1 + 10, cy, w2, c2, CIAN);
        }

        void chip(Graphics2D g, double x, double y, double w, String txt, Color c) {
            RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, 30, 30, 30);
            g.setColor(alfa(c, 34));
            g.fill(r);
            g.setColor(alfa(c, 170));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(r);
            g.setFont(base.deriveFont(Font.BOLD, 12.5f));
            g.setColor(TEXTO);
            centrar(g, txt, x + w / 2, y + 15);
        }

        void codigo(Graphics2D g, Snap s) {
            Font f = new Font(MONO, Font.PLAIN, 13).deriveFont(13.5f);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int cw = fm.charWidth('M');
            int x0 = CODE_X + 24, y0 = BOT_Y + 62, lh = 17;
            String[] codigoActual = modelo.comparacion ? CODIGO_COMPARACION : CODIGO;
            for (int i = 0; i < codigoActual.length; i++) {
                int y = y0 + i * lh;
                double a = lineaA[i];
                if (a > 0.02) {
                    g.setColor(alfa(CIAN, (int) (58 * a)));
                    g.fill(new RoundRectangle2D.Double(CODE_X + 12, y - 13, CODE_W - 24, lh, 8, 8));
                    g.setColor(alfa(CIAN, (int) (255 * a)));
                    g.fill(new RoundRectangle2D.Double(CODE_X + 12, y - 13, 3.5, lh, 3, 3));
                }
                String linea = codigoActual[i];
                int c = linea.indexOf("//");
                String cod = c >= 0 ? linea.substring(0, c).replaceAll("\\s+$", "") : linea;
                g.setColor(mezcla(new Color(165, 182, 232), Color.WHITE, a));
                g.drawString(cod, x0, y);
                if (c >= 0) {
                    g.setColor(new Color(105, 150, 140));
                    g.drawString(linea.substring(c), x0 + 46 * cw, y);
                }
            }
        }

        // ---------------- piezas de dibujo ----------------
        void fantasma(Graphics2D g, double x, double y, double r, Color c) {
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 4f, 5f }, 0f));
            g.setColor(c);
            g.draw(new Ellipse2D.Double(x - r + 1, y - r + 1, 2 * r - 2, 2 * r - 2));
        }

        void segmento(Graphics2D g, double x1, double y1, double x2, double y2, double recorte, Color c) {
            double dx = x2 - x1, dy = y2 - y1, d = Math.hypot(dx, dy);
            if (d < 2 * recorte) return;
            double ux = dx / d, uy = dy / d;
            g.setColor(c);
            g.draw(new Line2D.Double(x1 + ux * recorte, y1 + uy * recorte, x2 - ux * recorte, y2 - uy * recorte));
        }

        /** Flecha punteada con punta solida, como las de la imagen de referencia. */
        void flecha(Graphics2D g, double x1, double y1, double r1, double x2, double y2, double r2, Color c) {
            double dx = x2 - x1, dy = y2 - y1, d = Math.hypot(dx, dy);
            if (d < r1 + r2 + 16) return;
            double ux = dx / d, uy = dy / d;
            double sx = x1 + ux * (r1 + 3), sy = y1 + uy * (r1 + 3);
            double ex = x2 - ux * (r2 + 3), ey = y2 - uy * (r2 + 3);
            double largo = 9, ancho = 5.5;
            double bx = ex - ux * largo, by = ey - uy * largo;
            g.setColor(c);
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 6f, 6f }, 0f));
            g.draw(new Line2D.Double(sx, sy, bx, by));
            g.setStroke(new BasicStroke(1f));
            Path2D.Double punta = new Path2D.Double();
            punta.moveTo(ex, ey);
            punta.lineTo(bx - uy * ancho, by + ux * ancho);
            punta.lineTo(bx + uy * ancho, by - ux * ancho);
            punta.closePath();
            g.fill(punta);
        }

        void corchete(Graphics2D g, double x, double y, boolean izq, double alpha) {
            double h = 23, d = izq ? 7 : -7;
            Path2D.Double p = new Path2D.Double();
            p.moveTo(x + d, y - h);
            p.lineTo(x, y - h);
            p.lineTo(x, y + h);
            p.lineTo(x + d, y + h);
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alfa(Color.WHITE, (int) (225 * Math.min(1, alpha))));
            g.draw(p);
        }

        /** Triangulo rojo con etiqueta (como el puntero de la imagen de referencia). */
        void puntero(Graphics2D g, double x, double y, String texto, double alpha) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, alpha)));
            Path2D.Double tri = new Path2D.Double();
            tri.moveTo(x, y);
            tri.lineTo(x - 7.5, y + 13);
            tri.lineTo(x + 7.5, y + 13);
            tri.closePath();
            g.setColor(ROJO);
            g.fill(tri);
            g.setFont(base.deriveFont(Font.BOLD, 12.5f));
            centrar(g, texto, x, y + 27);
            g.setComposite(old);
        }

        static void centrar(Graphics2D g, String txt, double cx, double cy) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(txt, (float) (cx - fm.stringWidth(txt) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
        }

        static void derecha(Graphics2D g, String txt, double xr, double y) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(txt, (float) (xr - fm.stringWidth(txt)), (float) y);
        }

        static void envolver(Graphics2D g, String texto, double x, double y, double ancho, double alto) {
            FontMetrics fm = g.getFontMetrics();
            StringBuilder linea = new StringBuilder();
            for (String palabra : texto.split(" ")) {
                String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
                if (fm.stringWidth(prueba) > ancho && linea.length() > 0) {
                    g.drawString(linea.toString(), (float) x, (float) y);
                    y += alto;
                    linea = new StringBuilder(palabra);
                } else {
                    linea = new StringBuilder(prueba);
                }
            }
            if (linea.length() > 0) g.drawString(linea.toString(), (float) x, (float) y);
        }
    }

    /** Boton de pestana (mismo estilo que los botones). */
    @SuppressWarnings("serial")
    static final class Tab extends JButton {
        boolean activa;
        final int ancho;

        Tab(String texto, int ancho) {
            super(texto);
            this.ancho = ancho;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(new Font(FUENTE, Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override public Dimension getPreferredSize() { return new Dimension(ancho, 36); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean sobre = getModel().isRollover();
            RoundRectangle2D.Double forma = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, h, h);
            g.setColor(activa ? new Color(0, 208, 255, 80) : sobre ? new Color(0, 208, 255, 45) : new Color(22, 36, 92));
            g.fill(forma);
            g.setColor(activa ? CIAN : new Color(0, 208, 255, sobre ? 200 : 110));
            g.setStroke(new BasicStroke(activa ? 2f : 1.4f));
            g.draw(forma);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(activa ? Color.WHITE : TEXTO);
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Pestana "Complejidad (Big O)": tiempo, espacio, comparacion con los datos cargados y los tres casos. */
    @SuppressWarnings("serial")
    static final class ComplejidadPanel extends JPanel {
        final AnimPanel anim;
        final Font base = new Font(FUENTE, Font.PLAIN, 12);

        ComplejidadPanel(AnimPanel anim) {
            this.anim = anim;
            setPreferredSize(new Dimension(960, 540));
            new Timer(250, e -> { if (isShowing()) repaint(); }).start();   // se actualiza si cambian las listas
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int pw = getWidth(), ph = getHeight();
            g.setColor(FONDO);
            g.fillRect(0, 0, pw, ph);
            if (anim.modelo != null && pw > 0 && ph > 0) {
                double sc = Math.min(pw / (double) W, ph / (double) H);
                g.translate((pw - W * sc) / 2, (ph - H * sc) / 2);
                g.scale(sc, sc);
                escena(g);
            }
            g.dispose();
        }

        void tarjeta(Graphics2D g, int x, int y, int w, int h, String titulo, Color c) {
            g.setColor(new Color(26, 46, 134));
            g.fill(new RoundRectangle2D.Double(x - 7, y + 7, w, h, 22, 22));
            RoundRectangle2D.Double t = new RoundRectangle2D.Double(x, y, w, h, 22, 22);
            g.setColor(new Color(8, 15, 45));
            g.fill(t);
            g.setColor(new Color(70, 112, 235));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(t);
            g.setFont(base.deriveFont(Font.BOLD, 17f));
            g.setColor(c);
            g.drawString(titulo, x + 20, y + 31);
        }

        /** Tarjeta con el Big O en grande y su explicacion. */
        void bloque(Graphics2D g, int x, int y, int w, int h, String titulo, Color c, String gran, String texto) {
            tarjeta(g, x, y, w, h, titulo, c);
            g.setFont(new Font(MONO, Font.BOLD, 40));
            g.setColor(c);
            g.drawString(gran, x + 20, y + 92);
            g.setFont(base.deriveFont(Font.PLAIN, 15.5f));
            g.setColor(TEXTO);
            AnimPanel.envolver(g, texto, x + 20, y + 134, w - 40, 23);
        }

        static String dec(double v) { return String.format("%.2f", v).replace('.', ','); }

        void escena(Graphics2D g) {
            boolean cmp = anim.modelo.comparacion;
            int k = anim.k, n = anim.modelo.total;
            g.setFont(base.deriveFont(Font.BOLD, 26f));
            g.setColor(ROSA);
            g.drawString("Complejidad en notaci\u00f3n Big O", 28, 36);
            g.setFont(base.deriveFont(Font.PLAIN, 13.5f));
            g.setColor(TENUE);
            g.drawString((cmp ? "Comparaci\u00f3n uno por uno" : "Algoritmo voraz con Min-heap")
                    + ".  N = nodos en total, k = n\u00famero de listas.", 28, 57);

            int[] xs = { 24, 440, 856 };
            int cw = 400;

            // ---- fila 1: tiempo, espacio y comparacion con los datos
            int y1 = 70, h1 = 334;
            bloque(g, xs[0], y1, cw, h1, "Tiempo", CIAN, cmp ? "O(N \u00b7 k)" : "O(N log k)", cmp
                    ? "Para sacar cada uno de los N nodos se comparan las k cabezas una por una. Es simple, pero se vuelve lento cuando k crece: N \u00b7 k comparaciones."
                    : "Cada uno de los N nodos entra una vez al heap y sale una vez. Como el heap guarda a lo sumo k nodos, cada operaci\u00f3n cuesta O(log k). En total: N \u00b7 log k.");
            bloque(g, xs[1], y1, cw, h1, "Espacio extra", MENTA, cmp ? "O(1)" : "O(k)", cmp
                    ? "Solo se usan unas pocas variables (menor, cola). No hay estructura auxiliar, pero se recorren las k cabezas en cada paso."
                    : "El heap guarda como m\u00e1ximo una cabeza por lista: k nodos. Los nodos se reenlazan, no se copian, as\u00ed que el resultado no pide memoria nueva.");

            tarjeta(g, xs[2], y1, cw, h1, "Con estos datos (operaciones aprox.)", AMBAR);
            int x3 = xs[2] + 20;
            if (n == 0 || k <= 1) {
                g.setFont(base.deriveFont(Font.PLAIN, 15.5f));
                g.setColor(TEXTO);
                AnimPanel.envolver(g, n == 0
                                ? "No hay nodos que mezclar: no se hace ning\u00fan paso."
                                : "Con una sola lista no hay nada que comparar: se recorre en N = " + n + " pasos.",
                        x3, y1 + 80, cw - 40, 23);
            } else {
                double lk = Math.log(k) / Math.log(2), ln = Math.log(n) / Math.log(2);
                double[] v = { n * lk, (double) n * k, n * Math.max(1, ln) };
                String[] nom = { "Heap: N log k", "Uno por uno: N \u00b7 k", "Juntar y ordenar: N log N" };
                Color[] col = { CIAN, ROSA, GRIS };
                double max = Math.max(1, Math.max(v[0], Math.max(v[1], v[2])));
                for (int i = 0; i < 3; i++) {
                    boolean activo = (i == 0 && !cmp) || (i == 1 && cmp);
                    double yy = y1 + 82 + i * 58;
                    g.setFont(base.deriveFont(activo ? Font.BOLD : Font.PLAIN, 14.5f));
                    g.setColor(activo ? TEXTO : TENUE);
                    g.drawString(nom[i], (float) x3, (float) yy);
                    double bw = Math.max(5, 255 * v[i] / max);
                    g.setColor(alfa(col[i], activo ? 255 : 130));
                    g.fill(new RoundRectangle2D.Double(x3, yy + 9, bw, 18, 9, 9));
                    g.setColor(TEXTO);
                    g.drawString("\u2248 " + Math.round(v[i]), (float) (x3 + bw + 10), (float) (yy + 24));
                }
                g.setFont(base.deriveFont(Font.PLAIN, 13f));
                g.setColor(TENUE);
                g.drawString("log2 k = " + dec(lk) + "     log2 N = " + dec(ln), (float) x3, (float) (y1 + 276));
                g.drawString("La fila resaltada es el algoritmo cargado.", (float) x3, (float) (y1 + 297));
            }

            // ---- fila 2: mejor, promedio y peor caso
            int y2 = 416, h2 = 292;
            String[] gran = cmp ? new String[] { "O(N)", "O(N \u00b7 k)", "O(N \u00b7 k)" }
                    : new String[] { "O(N)", "O(N log k)", "O(N log k)" };
            String[] texto = cmp ? new String[] {
                    "Con k = 1 hay una sola cabeza que mirar en cada paso, as\u00ed que basta recorrer los N nodos.",
                    "En cada paso se miran todas las listas, as\u00ed que el costo sigue siendo del orden de N \u00b7 k aunque algunas se vayan vaciando.",
                    "Con k listas largas, cada uno de los N nodos obliga a comparar k cabezas. Crece lineal con k, a diferencia del heap (log k)." }
                    : new String[] {
                    "Con k = 1 no hay nada que reordenar: solo se recorren los N nodos. Con valores repetidos el heap casi no se reordena.",
                    "Cada nodo entra y sale del heap, y cada operaci\u00f3n sube o baja por un heap de alto log k. Con datos al azar sigue siendo del orden de N log k.",
                    "Cada extracci\u00f3n baja hasta el fondo del heap (log k niveles) para los N nodos. Nunca se vuelve atr\u00e1s, as\u00ed que no hay nada peor." };
            String[] nombre = { "Mejor caso", "Caso promedio", "Peor caso" };
            Color[] c2 = { MENTA, AMBAR, ROJO };
            for (int i = 0; i < 3; i++) bloque(g, xs[i], y2, cw, h2, nombre[i], c2[i], gran[i], texto[i]);
        }
    }

    // =====================================================================
    //  VENTANA Y CONTROLES
    // =====================================================================

    /** Boton plano con icono dibujado (no depende de que la fuente tenga simbolos). */
    @SuppressWarnings("serial")
    static final class Btn extends JButton {
        static final int NINGUNO = 0, PLAY = 1, PAUSA = 2, ANTERIOR = 3, SIGUIENTE = 4, REINICIAR = 5, ABAJO = 6;
        int icono;
        final int ancho;

        Btn(String texto, int icono, int ancho) {
            super(texto);
            this.icono = icono;
            this.ancho = ancho;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(new Font(FUENTE, Font.BOLD, 14));
            setForeground(TEXTO);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override public Dimension getPreferredSize() { return new Dimension(ancho, 38); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            ButtonModel m = getModel();
            RoundRectangle2D.Double forma = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, h, h);
            g.setColor(m.isPressed() ? new Color(0, 160, 215, 210) : m.isRollover() ? new Color(0, 208, 255, 85) : new Color(22, 36, 92));
            g.fill(forma);
            g.setColor(new Color(0, 208, 255, m.isRollover() ? 235 : 140));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(forma);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(getText());
            int iw = icono == NINGUNO ? 0 : 22;
            int x = (w - (tw + iw)) / 2;
            if (icono != NINGUNO) dibujarIcono(g, icono, x + 7, h / 2.0);
            g.setColor(TEXTO);
            g.drawString(getText(), x + iw, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }

        static void dibujarIcono(Graphics2D g, int tipo, double cx, double cy) {
            g.setColor(Color.WHITE);
            Path2D.Double p = new Path2D.Double();
            switch (tipo) {
                case PLAY:
                    p.moveTo(cx - 5, cy - 7); p.lineTo(cx + 6, cy); p.lineTo(cx - 5, cy + 7); p.closePath();
                    g.fill(p);
                    break;
                case PAUSA:
                    g.fill(new Rectangle2D.Double(cx - 6, cy - 7, 4.5, 14));
                    g.fill(new Rectangle2D.Double(cx + 1.5, cy - 7, 4.5, 14));
                    break;
                case ANTERIOR:
                    p.moveTo(cx + 5, cy - 7); p.lineTo(cx - 5, cy); p.lineTo(cx + 5, cy + 7); p.closePath();
                    g.fill(p);
                    break;
                case SIGUIENTE:
                    p.moveTo(cx - 5, cy - 7); p.lineTo(cx + 5, cy); p.lineTo(cx - 5, cy + 7); p.closePath();
                    g.fill(p);
                    break;
                case REINICIAR:
                    g.fill(new Rectangle2D.Double(cx - 7, cy - 7, 2.6, 14));
                    p.moveTo(cx + 6, cy - 7); p.lineTo(cx - 4, cy); p.lineTo(cx + 6, cy + 7); p.closePath();
                    g.fill(p);
                    break;
                case ABAJO:
                    p.moveTo(cx - 5, cy - 3); p.lineTo(cx + 5, cy - 3); p.lineTo(cx, cy + 4); p.closePath();
                    g.fill(p);
                    break;
                default:
                    break;
            }
        }
    }

    static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(TENUE);
        l.setFont(new Font(FUENTE, Font.PLAIN, 14));
        return l;
    }

    static void atajo(InputMap im, ActionMap am, final JTextField campo, String nombre, int tecla, final Runnable accion) {
        im.put(KeyStroke.getKeyStroke(tecla, 0), nombre);
        am.put(nombre, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!campo.hasFocus()) accion.run();          // si estas escribiendo, no se activan
            }
        });
    }

    static String formatoVel(double v) {
        return (v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v)) + "\u00d7";
    }

    static void crearVentana() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se queda con el aspecto por defecto
        }
        Color fondoMenu = new Color(12, 20, 56);
        UIManager.put("PopupMenu.background", fondoMenu);
        UIManager.put("PopupMenu.border", new LineBorder(new Color(60, 90, 170)));
        UIManager.put("MenuItem.background", fondoMenu);
        UIManager.put("MenuItem.foreground", TEXTO);
        UIManager.put("MenuItem.selectionBackground", new Color(0, 130, 180));
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.font", new Font(FUENTE, Font.PLAIN, 14));
        UIManager.put("ToolTip.background", fondoMenu);
        UIManager.put("ToolTip.foreground", TEXTO);

        final AnimPanel panel = new AnimPanel();
        final JTextField campo = new JTextField(EJEMPLOS[0][1], 38);
        final JLabel msg = new JLabel(" ");
        final boolean[] comparacionActiva = { false };
        final Btn bReiniciar = new Btn("Reiniciar", Btn.REINICIAR, 122);
        final Btn bAnterior = new Btn("Anterior", Btn.ANTERIOR, 116);
        final Btn bPlay = new Btn("Reproducir", Btn.PLAY, 140);
        final Btn bSiguiente = new Btn("Siguiente", Btn.SIGUIENTE, 124);
        final Btn bMenos = new Btn("-", Btn.NINGUNO, 38);
        final Btn bMas = new Btn("+", Btn.NINGUNO, 38);
        final Btn bEjemplos = new Btn("Ejemplos", Btn.ABAJO, 128);
        final Btn bAplicar = new Btn("Aplicar", Btn.NINGUNO, 92);
        final JLabel lblVel = new JLabel("1\u00d7", SwingConstants.CENTER);
        lblVel.setForeground(TEXTO);
        lblVel.setFont(new Font(FUENTE, Font.BOLD, 15));
        lblVel.setPreferredSize(new Dimension(44, 30));

        campo.setBackground(new Color(12, 20, 56));
        campo.setForeground(TEXTO);
        campo.setCaretColor(CIAN);
        campo.setSelectionColor(new Color(0, 120, 180));
        campo.setFont(new Font(MONO, Font.PLAIN, 14));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 90, 170), 1, true), new EmptyBorder(6, 10, 6, 10)));
        campo.setToolTipText("Listas ordenadas entre corchetes, por ejemplo [1,4,7],[2,5,8],[3,6,9]. "
                + "Una lista vac\u00eda es []. Vac\u00edo = sin listas.");
        msg.setFont(new Font(FUENTE, Font.PLAIN, 13));
        msg.setForeground(TENUE);

        panel.alCambiar = new Runnable() {
            @Override public void run() {
                bPlay.setText(panel.reproduciendo ? "Pausa" : "Reproducir");
                bPlay.icono = panel.reproduciendo ? Btn.PAUSA : Btn.PLAY;
                bPlay.repaint();
                lblVel.setText(formatoVel(AnimPanel.VELOCIDADES[panel.velIdx]));
            }
        };

        final Runnable aplicar = new Runnable() {
            @Override public void run() {
                try {
                    int[][] d = parse(campo.getText());
                    int n = 0;
                    for (int[] l : d) n += l.length;
                    panel.cargar(d, comparacionActiva[0]);
                    msg.setForeground(TENUE);
                    msg.setText("Cargado: k = " + d.length + ", N = " + n);
                } catch (IllegalArgumentException ex) {
                    msg.setForeground(ROJO);
                    msg.setText(ex.getMessage());
                }
                panel.requestFocusInWindow();
            }
        };

        final JPopupMenu menu = new JPopupMenu();
        for (final String[] ej : EJEMPLOS) {
            JMenuItem mi = new JMenuItem(ej[0]);
            mi.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    campo.setText(ej[1]);
                    comparacionActiva[0] = ej[0].equals("Comparación uno por uno");
                    aplicar.run();
                }
            });
            menu.add(mi);
        }

        bReiniciar.addActionListener(e -> panel.reiniciar());
        bAnterior.addActionListener(e -> panel.anterior());
        bSiguiente.addActionListener(e -> panel.siguiente());
        bPlay.addActionListener(e -> panel.alternar());
        bMenos.addActionListener(e -> panel.cambiarVelocidad(-1));
        bMas.addActionListener(e -> panel.cambiarVelocidad(1));
        bAplicar.addActionListener(e -> aplicar.run());
        campo.addActionListener(e -> aplicar.run());
        bEjemplos.addActionListener(e -> menu.show(bEjemplos, 0, -menu.getPreferredSize().height));

        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        fila1.setOpaque(false);
        fila1.add(bReiniciar);
        fila1.add(bAnterior);
        fila1.add(bPlay);
        fila1.add(bSiguiente);
        fila1.add(Box.createHorizontalStrut(24));
        fila1.add(etiqueta("Velocidad"));
        fila1.add(bMenos);
        fila1.add(lblVel);
        fila1.add(bMas);

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        fila2.setOpaque(false);
        fila2.add(bEjemplos);
        fila2.add(etiqueta("Listas"));
        fila2.add(campo);
        fila2.add(bAplicar);
        fila2.add(msg);

        JPanel barra = new JPanel();
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBackground(FONDO);
        barra.setBorder(new EmptyBorder(9, 8, 9, 8));
        barra.add(fila1);
        barra.add(Box.createVerticalStrut(8));
        barra.add(fila2);

        JFrame f = new JFrame("Mezcla de k listas ordenadas: algoritmo voraz con Min-heap");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().setLayout(new BorderLayout());
        final ComplejidadPanel cxPanel = new ComplejidadPanel(panel);
        final CardLayout cartas = new CardLayout();
        final JPanel centro = new JPanel(cartas);
        centro.setBackground(FONDO);
        centro.add(panel, "anim");
        centro.add(cxPanel, "cx");
        final Tab tAnim = new Tab("Animaci\u00f3n", 150), tCx = new Tab("Complejidad (Big O)", 210);
        tAnim.activa = true;
        tAnim.addActionListener(e -> {
            cartas.show(centro, "anim");
            tAnim.activa = true; tCx.activa = false;
            fila1.setVisible(true);
            fila2.setVisible(true);
            tAnim.repaint(); tCx.repaint();
            panel.requestFocusInWindow();
        });
        tCx.addActionListener(e -> {
            cartas.show(centro, "cx");
            tAnim.activa = false; tCx.activa = true;
            fila1.setVisible(false);
            fila2.setVisible(false);
            tAnim.repaint(); tCx.repaint();
        });
        JPanel pestanas = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pestanas.setBackground(FONDO);
        pestanas.setBorder(new EmptyBorder(8, 12, 4, 12));
        f.getContentPane().setBackground(FONDO);
        barra.setPreferredSize(barra.getPreferredSize());    // la barra conserva su alto aunque se oculte su contenido
        pestanas.add(tAnim);
        pestanas.add(tCx);
        f.getContentPane().add(pestanas, BorderLayout.NORTH);
        f.getContentPane().add(centro, BorderLayout.CENTER);
        f.getContentPane().add(barra, BorderLayout.SOUTH);

        InputMap im = f.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = f.getRootPane().getActionMap();
        atajo(im, am, campo, "alternar", KeyEvent.VK_SPACE, () -> panel.alternar());
        atajo(im, am, campo, "siguiente", KeyEvent.VK_RIGHT, () -> panel.siguiente());
        atajo(im, am, campo, "anterior", KeyEvent.VK_LEFT, () -> panel.anterior());
        atajo(im, am, campo, "masRapido", KeyEvent.VK_UP, () -> panel.cambiarVelocidad(1));
        atajo(im, am, campo, "masLento", KeyEvent.VK_DOWN, () -> panel.cambiarVelocidad(-1));
        atajo(im, am, campo, "reiniciar", KeyEvent.VK_R, () -> panel.reiniciar());

        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        f.setSize(Math.min(1280, pantalla.width - 40), Math.min(830, pantalla.height - 60));
        f.setMinimumSize(new Dimension(1080, 640));
        f.setLocationRelativeTo(null);

        panel.cargar(parse(EJEMPLOS[0][1]));
        f.setVisible(true);
        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MezclaKListas::crearVentana);
    }
}
