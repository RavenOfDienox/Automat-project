package project;

import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;
import java.util.ArrayList;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.atan2;
import static java.lang.Math.ceil;
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.hypot;
import static java.lang.Math.pow;


public class GraphComponent extends JPanel {
    
    private final ArrayList<Node> nodes;        // Расширяемый массив узлов
    private final ArrayList<Connection> connections;  // Массив переходов
    private int node_diam;                      // Диаметр узлов
    public double min_dist;                     // Минимальное расстояние между узлами
    private double arc_scale;
    private int current_elem_index;             // Индекс активного элемента
    private int enter_index;                    // Индекс узла, являющегося входом
    private int offset_x, offset_y;             // Отклонение для перемещения вида
    private int net_scale;                      // Размер сетки
    private Color net_color;                    // Цвет сетки
    private Font font;                          // Шрифт
    private FontMetrics font_metrics;           // Размеры шрифта
    private String letter;                      // Строка для переходов
    private Point current_arrow;                // Конечная точка активного перехода
    public boolean show_net;                    // Нужно ли показывать сетку
    public boolean show_name;                   // Нужно ли показывать имя узла
    
    public GraphComponent(int diam) {
        nodes = new ArrayList<>();
        connections = new ArrayList<>();
        node_diam = diam;
        min_dist = node_diam * 1.5;
        current_elem_index = -1;
        enter_index = -1;
        offset_x = 0;
        offset_y = 0;
        net_scale = 80;
        net_color = new Color(0, 0, 0, (float)0.05);
        font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        letter = "";
        current_arrow = null;
        arc_scale = 3;
        show_net = true;
        show_name = false;
    }
    
    // Добавляет узел
    public void addElem(int x, int y) {
        // Не позволяет добавить элемент если расстояние
        // до другого элемента слишком маленькое
        if (!tooClose(x, y))
            // Добавление узла с центром в точке (x, y)
            this.nodes.add(new Node(x-offset_x, y-offset_y));
    }
    
    // Удаляет узел и соединения других узлов с ним
    public void removeElem(int x, int y) {
        int index = getElemIndexAt(x, y);
        if (index != -1) {
            // Убрать входной узел, если есть
            if (enter_index == index) enter_index = -1;
            
            // Удаление соединений, связанных с узлом
            Node node = nodes.get(index);
            for (int i = connections.size()-1; i >= 0; i--) {
                Connection conn = connections.get(i);
                Node from = conn.getFrom();
                Node to = conn.getTo();
                if (node == from || node == to)
                    connections.remove(i);
            }
            
            // Убрать сам узел
            nodes.remove(index);
        }
    }   
    
    // Двигает узел (пока зажата кнопка мыши)
    public void moveElem(int x, int y) {
        chooseActiveNode(x, y);
        if (current_elem_index != -1) {
            Node node = nodes.get(current_elem_index);
            node.set(x - offset_x, y - offset_y);
        }
    }
    
    // Фиксирует узел. Использовать после moveElem (когда кнопка мыши отпущена),
    // чтобы выбрать другой узел для перемещения
    public void fixElem(int x, int y) {
        if (current_elem_index != -1) {
            Node node = nodes.get(current_elem_index);
            node.set(x - offset_x, y - offset_y);
            nodes.set(current_elem_index, node); // Нужно ли?
            // Конец работы с активным узлом
            current_elem_index = -1;
        }
    }
    
    // В режиме добавления соединения при нажатии кнопки мыши
    public void addArrow(int x, int y) {
        chooseActiveNode(x, y);
        if (current_elem_index != -1 && letter.length() != 0) {
            current_arrow = new Point(x - offset_x, y - offset_y);
        }
    }
    
    // В режиме добавления соединения при перемещении мыши
    public void moveArrow(int x, int y) {
        if (current_arrow != null) {
            current_arrow = new Point(x - offset_x, y - offset_y);
        }
    }
    
    // В режиме добавления соединения при отпускании мыши
    public void fixArrow(int x, int y) {
        if (current_arrow != null) {
            
            // Если кнопка мыши была отпущена на существующем узле
            int other_node_index = getElemIndexAt(x, y);
            if (other_node_index != -1 && other_node_index != current_elem_index) {
                Node node = nodes.get(current_elem_index);
                Node other_node = nodes.get(other_node_index);
                
                // Проверка на существование второго такого же перехода к другому узлу,
                // проверка на существование обратного перехода
                boolean has_reverse = false;
                for (int i = connections.size()-1; i >= 0; i--) {
                    Connection conn = connections.get(i);
                    Node from = conn.getFrom();
                    Node to = conn.getTo();
                    char firstChar = conn.getWeight().charAt(0);
                    if (from == node && firstChar == letter.charAt(0)) {
                        if (conn.has_reverse) {
                            for (Connection rev : connections) {
                                if (rev.has_reverse)
                                    // Работает - не трогай
                                    // Это эффективно, честно
                                    if (rev.getFrom() == to && rev.getTo() == from) {
                                        rev.has_reverse = false;
                                        break;
                                    }
                            }
                        }
                        connections.remove(i);
                    }
                    if (node == to && other_node == from) {
                        has_reverse = true;
                        conn.has_reverse = true;
                    }
                }
                
                // Подключение к существующему узлу
                Connection conn = new Connection(node, other_node, letter);
                conn.has_reverse = has_reverse;
                connections.add(conn);
            }
            // Конец работы с активным узлом
            current_elem_index = -1;
            // Конец работы с активным переходом
            current_arrow = null;
        }
    }
    
    // Добавление соединения с самим собой
    public void addLoop(int x, int y) {
        int index = getElemIndexAt(x, y);
        if (index != -1 && letter.length() != 0) {
            
            Node node = nodes.get(index);
            // Проверка на существование второго такого же перехода к другому узлу,
            // проверка на соединение с самим собой
            for (int i = connections.size()-1; i >= 0; i--) {
                Connection conn = connections.get(i);
                Node from = conn.getFrom();
                Node to = conn.getTo();
                char firstChar = conn.getWeight().charAt(0);
                if (from == node && firstChar == letter.charAt(0)) {
                    if (conn.has_reverse) {
                        for (Connection rev : connections) {
                            if (rev.has_reverse)
                                // Работает - не трогай
                                if (rev.getFrom() == to && rev.getTo() == from)
                                    rev.has_reverse = false;
                        }
                    }
                    connections.remove(i);
                }
                else if (node == from && from == to)
                    connections.remove(i);
                
            }
            
            Connection conn = new Connection(node, node, letter);
            connections.add(conn);
            
        }
    }
    
    // Удаляет петлю
    public void removeLoop(int x, int y) {
        int index = getElemIndexAt(x, y);
        if (index != -1) {
            Node node = nodes.get(index);
            for(int i = 0; i < connections.size(); i++) {
                Connection conn = connections.get(i);
                Node from = conn.getFrom();
                Node to = conn.getTo();
                if (node == from && from == to) {
                    connections.remove(i);
                    break;
                }
            }
        }
    }
    
    // Выбор активного узла
    public void chooseActiveNode(int x, int y) {
        if (current_elem_index == -1)
            current_elem_index = getElemIndexAt(x, y);
    }
    
    // Выбор узла конца соединения для его удаления
    public void removeTo(int x, int y) {
        
        int other_elem_index = -1;
        if (current_elem_index != -1)
            other_elem_index = getElemIndexAt(x, y);
        
        if (other_elem_index != -1) {
            
            // Выбранные пользователем узлы
            Node from = nodes.get(current_elem_index);
            Node to = nodes.get(other_elem_index);
            
            for (int i = connections.size()-1; i >= 0; i--) {
                Connection conn = connections.get(i);
                Node from2 = conn.getFrom();
                Node to2 = conn.getTo();
                
                // Сравнение элементов соединения с выбранными узлами
                if (from == from2 && to == to2) {
                    // Подготовка к удалению - проверка на наличие обратного соединения
                    if (conn.has_reverse) {
                        for (Connection rev : connections) {
                            if (rev.has_reverse) {
                                if (rev.getFrom() == to && rev.getTo() == from) {
                                    rev.has_reverse = false;
                                    break;
                                }
                            }
                        }
                    }
                    // Удаление
                    connections.remove(i);
                    break;
                }
            }
        }
        // Конец работы с активным элементом
        current_elem_index = -1;
    }
    
    // Устанавливает вход в одном из узлов
    public void setEnter(int x, int y) {
        int index = getElemIndexAt(x, y);
        if (index != -1) {
            Node node;
            // Вход может быть только один, поэтому
            // если вход уже существует, убрать вход
            if (enter_index != -1) {
                node = nodes.get(enter_index);
                node.enter = false;
                nodes.set(enter_index, node);
            }
            // Установить новый вход
            node = nodes.get(index);
            node.enter = true;
            nodes.set(index, node);
            enter_index = index;
        }
    }
    
    // Устанавливает/убирает выход в выбранном узле
    public void setExit(int x, int y) {
        int index = getElemIndexAt(x, y);
        if (index != -1) {
            Node node = nodes.get(index);
            node.exit = !node.exit;
            nodes.set(index, node);
        }
    }
    
    public void setOffset(int offset_x, int offset_y) {
        this.offset_x = offset_x;
        this.offset_y = offset_y;
    }
    
    public void setLetter(String s) {
        letter = s;
    }
    
    // Возвращает смещение
    public String getOffset() {
        return offset_x + " " + offset_y;
    }
    
    // Возвращает регулярное выражение
    public String getRegular() throws RuntimeException {
        ArrayList<Node> nodes_copy = new ArrayList<>(nodes);
        ArrayList<Connection> conns_copy = new ArrayList<>(connections);
        
        // Проверка на наличие входа и выходов
        // Соединение выходов с единственным узлом
        Node enter = null;
        Node exit = new Node(0, 0);
        boolean exit_exists = false;
        for (Node node : nodes_copy) {
            if (node.enter)
                enter = node;
            if (node.exit) {
                exit_exists = true;
                Connection conn = new Connection(node, exit, "()");
                conns_copy.add(conn);
            }
        }        
        
        // Если вход или выход не найден
        if (enter == null) throw new RuntimeException();
        if (!exit_exists) throw new RuntimeException();
        
        // Пока не останутся только входной и выходной узлы
        while (nodes_copy.size() > 1) {
            
            // Выбрать узел для удаления
            Node node_to_remove = null;
            int node_rem_index = -1;
            int min_max_weight = 99999;
            for (int i = 0; i < nodes_copy.size(); i++) {
                Node node = nodes_copy.get(i);
                if (node.enter) continue;
                
                int max_weight = 0;
                for(Connection conn: conns_copy) {
                    // Все соединения с этим узлом
                    if (conn.getFrom() == node || conn.getTo() == node) {
                        // Выбирается строка с наибольшей длиной
                        String weight = conn.getWeight();
                        if (max_weight < weight.length())
                            max_weight = weight.length();                        
                    }
                }
                
                // Затем выбирается наименьная строка из наибольших.
                // Связанный с ней узел будет выбран для удаления.
                if (min_max_weight > max_weight) {
                    node_to_remove = node;
                    node_rem_index = i;
                    min_max_weight = max_weight;
                }
            }
            
            // Соединить каждый вход с каждым выходом
            // Для этого необходимо создать массивы всех входов и всех выходов
            ArrayList<Connection> enters = new ArrayList<>();
            ArrayList<Connection> exits = new ArrayList<>();
            ArrayList<Connection> conns_to_remove = new ArrayList<>();
            Connection loop = null;
            for (Connection conn : conns_copy) {
                Node from = conn.getFrom();
                Node to = conn.getTo();
                
                if (node_to_remove == from && from == to) {
                    loop = conn;
                    conns_to_remove.add(conn);
                }
                
                else if (node_to_remove == from) {
                    exits.add(conn);
                    conns_to_remove.add(conn);
                }
                
                else if (node_to_remove == to) {
                    enters.add(conn);
                    conns_to_remove.add(conn);
                }                    
            }
            
            // Соединение каждого входа с каждым выходом
            for (int i = 0; i < exits.size(); i++) {
                for (int j = 0; j < enters.size(); j++) {
                    Connection conn_from = enters.get(j);
                    Connection conn_to = exits.get(i);
                    Node from = conn_from.getFrom();
                    Node to = conn_to.getTo();
                    
                    String weight;
                    if (loop != null)
                        weight = conn_from.getWeight() + 
                                 "(" + loop.getWeight() + ")*" + 
                                 conn_to.getWeight();
                    else
                        weight = conn_from.getWeight() + 
                                 conn_to.getWeight();
                    
                    Connection conn = new Connection(from, to, weight);
                    conns_copy.add(conn);
                }
            }
            
            // Удаление соединений
            for (int i = conns_to_remove.size()-1; i >= 0; i--) {
                for (int j = conns_copy.size()-1; j >= 0; j--) {
                    Connection conn = conns_copy.get(j);
                    Connection conn_to_remove = conns_to_remove.get(i);
                    if (conn == conn_to_remove) {
                        conns_copy.remove(j);
                        conns_to_remove.remove(i);
                        break;
                    }
                }
            }
            
            // Удаление узла
            nodes_copy.remove(node_rem_index);
            
            // В конце объединить параллели
            for (int i = conns_copy.size()-1; i >= 1; i--) {
                
                Connection conn_1 = conns_copy.get(i);
                boolean has_parallel = false;
                for (int j = i-1; j >= 0; j--) {
                    Connection conn_2 = conns_copy.get(j);
                    
                    Node from_1 = conn_1.getFrom();
                    Node to_1 = conn_1.getTo();
                    Node from_2 = conn_2.getFrom();
                    Node to_2 = conn_2.getTo();
                    
                    if (from_1 == from_2 && to_1 == to_2) {
                        String weight;
                        if (!has_parallel) {
                            weight = "(" + conn_1.getWeight() + "|" + conn_2.getWeight();
                            has_parallel = true;
                        }
                        else weight = conn_1.getWeight() + "|" + conn_2.getWeight();
                        
                        conn_1.setWeight(weight);
                        conns_copy.remove(j);
                        i--;
                    }
                }
                if (has_parallel)
                    conn_1.setWeight(conn_1.getWeight() + ")");
            }
            
        } // Конец while
        
        // После всех манипуляций должно остаться 2 узла (вход и выход) и 1 или 2 соединения
        String regular;
        Connection conn_0 = conns_copy.get(0);
        Connection conn_1 = null;
        if (conns_copy.size() > 1)
            conn_1 = conns_copy.get(1);
        
        if (conn_1 == null) {
            if (conn_0.getFrom() == conn_0.getTo())
                regular = "(" + conn_0.getWeight() + ")*";
            else
                regular = conn_0.getWeight();
        }
        else {
            if (conn_0.getFrom() == conn_0.getTo())
                regular = "(" + conn_0.getWeight() + ")*" + conn_1.getWeight();
            else
                regular = "(" + conn_1.getWeight() + ")*" + conn_0.getWeight();
        }
        
        return regular;
    }
    
    // Возвращает индекс элемента, внутри которого есть точка (x, y)
    private int getElemIndexAt(int x, int y) {
                        
        for(int i = 0; i < nodes.size(); i++) {
            
            Node node = nodes.get(i);
            
            int node_x = node.getX() + offset_x;
            int node_y = node.getY() + offset_y;
            
            double distance = hypot((x - node_x), (y - node_y));
            
            if (distance < node_diam / 2) return i;
        }
        return -1;
    }
    
    // Возвращает true если точка ближе 
    // чем на минимальном расстоянии от одного из узлов
    private boolean tooClose(int x, int y) {
        for(int i = 0; i < nodes.size(); i++) {
            int node_x = nodes.get(i).getX();
            int node_y = nodes.get(i).getY();
            
            double distance = hypot((x - node_x), (y - node_y));
            
            if (distance < min_dist) return true;
        }
        return false;
    }
    
    // Рисует стрелочку с началом в точке (x1, y1) и с концом в точке (x2, y2)
    private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
        double alpha = atan2((x2 - x1), (y2 - y1));
        int left_wing_sin = (int)ceil(sin(PI/6 - alpha) * 7);
        int left_wing_cos = (int)ceil(cos(PI/6 - alpha) * 7);
        
        int right_wing_sin = (int)ceil(sin(PI/6 + alpha) * 7);
        int right_wing_cos = (int)ceil(cos(PI/6 + alpha) * 7);
        
        // Основание стрелки
        g.drawLine(x1, y1, x2, y2);
        // Левое крыло
        g.drawLine(x2, y2, x2+left_wing_sin, y2-left_wing_cos);
        // Правое крыло
        g.drawLine(x2, y2, x2-right_wing_sin, y2-right_wing_cos);
    }
    
    // Рисует дугу со стрелкой. Здесь я умер.
    private void drawArcArrow(Graphics g, int x1, int y1, int x2, int y2) {
        
        // Угол между двумя точками - это угол, на который нужно повернуть кривую
        double alpha = atan2((x2 - x1), (y2 - y1));
        // Кривая представляет собой дугу на эллипсе.
        // Ширина эллипса - его большая полуось * 2, высота - малая полуось * 2
        int arc_width = (int)hypot((x2 - x1), (y2 - y1));
        int arc_height = (int) (node_diam * arc_scale);
        
        // Центральная точка эллипса, на котором рисуется дуга
        int oval_mid_x = x1 + arc_width / 2;
        int oval_mid_y = y1;
        
        // Точка, через которую проходит касательная
        int tan_x = (int) (oval_mid_x + cos(PI/6) * arc_width/2);
        int tan_y = (int) (oval_mid_y - sin(PI/6) * arc_height/2);
        
        // Найти угол касательной
        double k = -(pow(arc_height, 2) / pow(arc_width, 2)) * (tan_x/tan_y);
        double ang = atan(k);
        
        // Необходимо использовать Graphics2D, чтобы повернуть дугу
        Graphics2D g2d = (Graphics2D) g;
        // Изначальное состояние, к которому необходимо будет вернуться после поворота
        AffineTransform reset = g2d.getTransform();
        // Поворот на нужный угол вокруг точки (x1, y1)
        g2d.rotate(-alpha+PI/2, x1, y1);
        // Сама дуга
        g2d.drawArc(x1, y1-arc_height/2, arc_width, arc_height, 30, 120);
        // "Крылья" стрелки, лежащей на касательной
        g2d.drawLine(tan_x, tan_y, (int) (tan_x-cos(ang + PI/6)*7), (int) (tan_y+sin(ang + PI/6)*7));
        g2d.drawLine(tan_x, tan_y, (int) (tan_x-cos(ang - PI/3)*7), (int) (tan_y+sin(ang - PI/3)*7));
        // Изначальное состояние (до поворота)
        g2d.setTransform(reset);
        
    }
    
    private void drawLoop(Graphics g, int x, int y, String weight) {
        
        int upper_left_x = x + node_diam / 2;
        int upper_left_y = y - node_diam;
        int width = node_diam * 4;
        int height = node_diam * 2;
        
        // Центр эллипса, на котором лежит дуга
        int mid_x = upper_left_x + width/2;
        int mid_y = upper_left_y + height/2;
        
        // Точка начала крыльев стрелки
        int arrow_x = (int) (mid_x + cos(-PI*5/6) * width/2);
        int arrow_y = (int) (mid_y - sin(-PI*5/6) * height/2);
        
        // Дуга
        g.drawArc(upper_left_x, upper_left_y, width, height, -150, 300);
        // "Крылья" стрелки
        g.drawLine(arrow_x, arrow_y, arrow_x + 7, arrow_y);
        g.drawLine(arrow_x, arrow_y, (int) (arrow_x + cos(-PI/3)*7), (int) (arrow_y - sin(-PI/3)*7));
        
        int x1 = upper_left_x + width;
        int y1 = upper_left_y;
        int x2 = x1;
        int y2 = y1 + height;
        // Надпись
        drawConnLetter(weight, g, x1, y1, x2, y2);
    }
    
    // Рисует строку перехода на середине стрелки внутри прямоугольника
    private void drawConnLetter(String letter, Graphics g, int x1, int y1, int x2, int y2) {
        int mid_x = x1 + (x2-x1)/2;
        int mid_y = y1 + (y2-y1)/2;
                
        int desc = font_metrics.getDescent();
        int rect_width = font_metrics.stringWidth(letter) + desc;
        int rect_height = font_metrics.getHeight();
        int rect_x = mid_x - rect_width / 2;
        int rect_y = mid_y - rect_height / 2;
        
        g.fillRect(rect_x, rect_y, rect_width, rect_height);
        
        int letter_x = mid_x - rect_width / 2 + desc / 2;
        int letter_y = mid_y + rect_height - font_metrics.getDescent() - rect_height / 2;
        g.setColor(Color.WHITE);
        g.drawString(letter, letter_x, letter_y);
        g.setColor(Color.BLACK);
    }
    
    // Установить цвет узла
    public void setNodeColor(Color color, int x, int y) {
        int index = getElemIndexAt(x, y);
        if (index != -1) {
            Node node = nodes.get(index);
            node.setColor(color);
        }
    }
    
    // Устанавливает цвет соединения
    public void setConnColor(Color color, int x, int y) {
        if (current_elem_index != -1) {
            int other_index = getElemIndexAt(x, y);
            if (other_index != -1) {
                Node from = nodes.get(current_elem_index);
                Node to = nodes.get(other_index);
                
                // Нахождение нужного соединения
                for (Connection conn : connections) {
                    if (conn.getFrom() == from && conn.getTo() == to) {
                        conn.setColor(color);
                        break;
                    }
                }
            }
            current_elem_index = -1;
        }
    }
    
    // Установить шрифт переходов
    public void setLetterFont(Font f) {
        font = f;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        
        // Размеры шрифта
        g.setFont(font);
        font_metrics = g.getFontMetrics(font);
        
        super.paintComponent(g); // Рисует панель
        
        // Отрисовка сетки
        if (show_net) {
            int net_offset_x = offset_x % net_scale;
            int net_offset_y = offset_y % net_scale;
            
            int this_width = this.getBounds().width;
            int this_height = this.getBounds().height;
            
            g.setColor(net_color);
            for (int x = net_offset_x; x < this_width; x += net_scale)
                g.drawLine(x, 0, x, this_height);
            for (int y = net_offset_y; y < this_height; y += net_scale)
                g.drawLine(0, y, this_width, y);
            
        }
        
        // Рисует все узлы
        Node node;
        for(int i = 0; i < nodes.size(); i++) {
            
            node = nodes.get(i);
            
            // Центральная точка
            int x = node.getX();
            int y = node.getY();
            
            // Левый верхний угол квадрата, в который вписан узел
            int x_top_left = x - (node_diam / 2) + offset_x;
            int y_top_left = y - (node_diam / 2) + offset_y;
            
            // Отрисовка узла
            g.setColor(node.getColor());
            g.drawOval(x_top_left, y_top_left, node_diam, node_diam);
            
            // Отрисовка выходов
            if (node.exit)
                drawArrow(g, 
                        x + offset_x, y-node_diam/2 + offset_y, 
                        x + offset_x, y-node_diam*2 + offset_y);
//            g.setColor(Color.BLACK);
        }
        
        // Отрисовка входа
        if (enter_index != -1) {
            node = nodes.get(enter_index);
            int x = node.getX();
            int y = node.getY();
            
            g.setColor(node.getColor());
            drawArrow(g, 
                    x-node_diam*2 + offset_x, y + offset_y, 
                    x-node_diam/2 + offset_x, y + offset_y);
        }
        
        // Отрисовка соединений
        for (Connection conn : connections) {
            Node from = conn.getFrom();
            Node to = conn.getTo();
            String weight = conn.getWeight();
            
            int from_x = from.getX();
            int from_y = from.getY();
            int to_x = to.getX();
            int to_y = to.getY();
            
            // Если переход к самому себе
            if (from.equals(to)) {
                g.setColor(conn.getColor());
                drawLoop(g, from_x+offset_x, from_y+offset_y, weight);
                continue;
            }
            
            // Если есть обратный переход
            if(conn.has_reverse) {

                int x1 = from_x + offset_x;
                int y1 = from_y + offset_y;
                int x2 = to_x + offset_x;
                int y2 = to_y + offset_y;
                
                // Отрисовка кривой со стрелкой
                g.setColor(conn.getColor());
                drawArcArrow(g, x1, y1, x2, y2);
                
                // Подпись к кривой
                double alpha = atan2((to_y - from_y), (to_x - from_x));
                int x1_w = (int)(x1 + sin(alpha) * node_diam * arc_scale / 2);
                int y1_w = (int)(y1 - cos(alpha) * node_diam * arc_scale / 2);
                int x2_w = (int)(x2 + sin(alpha) * node_diam * arc_scale / 2);
                int y2_w = (int)(y2 - cos(alpha) * node_diam * arc_scale / 2);
                
                // Отрисовка подписи
                g.setColor(Color.BLACK);
                drawConnLetter(weight, g, x1_w, y1_w, x2_w, y2_w);
            }
            else {
                
                // Чтобы линия начиналась не из центра
                double alpha = atan2((to_x - from_x), (to_y - from_y));
                int dimin_x = (int)(sin(alpha) * node_diam);
                int dimin_y = (int)(cos(alpha) * node_diam);

                int x1 = from_x + dimin_x + offset_x;
                int y1 = from_y + dimin_y + offset_y;
                int x2 = to_x - dimin_x + offset_x;
                int y2 = to_y - dimin_y + offset_y;
                
                // Стрелка
                g.setColor(conn.getColor());
                drawArrow(g, x1, y1, x2, y2);
                // Подпись к стрелке
                g.setColor(Color.BLACK);
                drawConnLetter(weight, g, x1, y1, x2, y2);
            }
        }
        
        // Отрисовка активного перехода
        if (current_arrow != null) {
            Node current_node = nodes.get(current_elem_index);
            
            int from_x = current_node.getX();
            int from_y = current_node.getY();
            int to_x = current_arrow.x;
            int to_y = current_arrow.y;
            
            // Чтобы линия начиналась не из центра
            double alpha = atan2((to_x - from_x), (to_y - from_y));
            int dimin_x = (int)(sin(alpha) * node_diam);
            int dimin_y = (int)(cos(alpha) * node_diam);
            
            int x1 = from_x + dimin_x + offset_x;
            int y1 = from_y + dimin_y + offset_y;
            int x2 = to_x - dimin_x + offset_x;
            int y2 = to_y - dimin_y + offset_y;
            
            // Отрисовка активной стрелки
            drawArrow(g, x1, y1, x2, y2);
            // Подпись к активной стрелке
            drawConnLetter(letter, g, x1, y1, x2, y2);
        }
    }
}