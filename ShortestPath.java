import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

class GraphNode
{
    Color color;
    boolean path;
    Point2D.Float point = new Point2D.Float();
}

class GraphEdge
{
    static final BasicStroke THIN = new BasicStroke(1.0f);
    static final BasicStroke THICK = new BasicStroke(6.0f);
    Color color;
    BasicStroke stroke;
    int nodeA;
    int nodeB;
    GraphEdge(int a, int b) {nodeA=a;nodeB=b;}
}

class Graph extends JComponent
{
    GraphNode[] nodes;
    GraphEdge[] edges;
    float minX, maxX;
    float minY, maxY;
    GraphEdge[][] mat;
    int rows, cols;

    static final float step = 10.0f;
    static final int radius = 10;

    Graph(int cols, int rows)
    {
        this.rows = rows;
        this.cols = cols;

        Random rand = new Random(0xcafebebe);
        nodes = new GraphNode[rows*cols];
        ArrayList<GraphEdge> dyn = new ArrayList<>();
        for (int y = 0; y < rows; ++y) {
            for (int x = 0; x < cols; ++x) {
                int i = x+y*cols;
                nodes[i] = new GraphNode();
                Point2D.Float p = nodes[i].point;
                p.x = x*step + rand.nextInt() % (step/3);
                p.y = y*step + rand.nextInt() % (step/3);
                if (x < cols-1) {
                    dyn.add(new GraphEdge(i,i+1));
                }
                if (y < rows-1) {
                    if (Math.abs(rand.nextInt()%1024) > 128)
                        dyn.add(new GraphEdge(i,i+cols));
                    else if (x < cols-1 && Math.abs(rand.nextInt()%1024) > 128) {
                        dyn.add(new GraphEdge(i+1,i+cols));
                    }
                }
                if (p.y > maxY)
                    maxY = p.y;
                else if (p.y < minY)
                    minY = p.y;
                if (p.x > maxX)
                    maxX = p.x;
                else if (p.x < minX)
                    minX = p.x;
            }
        }
        minX -= step;
        maxX += step;
        minY -= step;
        maxY += step;
        edges = new GraphEdge[dyn.size()];
        edges = dyn.toArray(edges);
        mat = new GraphEdge[nodes.length][nodes.length];
        for (GraphEdge e : edges) {
            int u = e.nodeA;
            int v = e.nodeB;
            mat[u][v] = e;
            mat[v][u] = e;
        }
    }

    int toview(float f, float min, float max, int step)
    {
        return (int)((f-min)/(max-min)*(float)step);
    }

    int toScreenX(float x, int w)
    {
        return toview(x, minX, maxX, w);
    }

    int toScreenY(float y, int h)
    {
        return toview(y, minY, maxY, h);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON); 
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();
        Rectangle clip = g.getClipBounds();
        g.setColor(Color.WHITE);
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        g.setColor(Color.BLACK);
        for (GraphEdge e : edges) {
            int u = e.nodeA;
            int v = e.nodeB;
            Point2D.Float p1 = nodes[u].point;
            Point2D.Float p2 = nodes[v].point;
            int x1 = toScreenX(p1.x, clip.width);
            int y1 = toScreenY(p1.y, clip.height);
            int x2 = toScreenX(p2.x, clip.width);
            int y2 = toScreenY(p2.y, clip.height);
            g.setColor(e.color);
            graphics2D.setStroke(e.stroke);
            g.drawLine(x1+radius, y1+radius, x2+radius, y2+radius);
            if (x1 > x2) {
                int tmp;
                tmp = x2; x2 = x1; x1 = tmp;
                tmp = y2; y2 = y1; y1 = tmp;
            }

            int x, y;
            x = x1+(x2-x1)/2;
            y = y1+(y2-y1)/2 + fm.getHeight();
            String text = String.valueOf((int)p1.distance(p2));
            g.setColor(Color.WHITE);
            Rectangle2D rect = fm.getStringBounds(text, g);
            x += (int)rect.getWidth()/2;
            y += fm.getDescent();
            g.fillRect(x, y - fm.getAscent(),
                    (int)rect.getWidth(),
                    (int)rect.getHeight());
            g.setColor(Color.BLACK);
            g.drawString(text, x, y);
        }

        graphics2D.setStroke(new BasicStroke(1.0F));
        for (int i = 0; i < nodes.length; ++i) {
            Point2D.Float p = nodes[i].point;
            int x = toScreenX(p.x, clip.width);
            int y = toScreenY(p.y, clip.height);

            String text = ""+(char)(i+'A');
            y += fm.getHeight();
            Rectangle2D rect = fm.getStringBounds(text, g);
            g.setColor(nodes[i].color);
            g.fillOval(
                    x - (int)rect.getWidth()/2,
                    y - fm.getHeight()-fm.getDescent(),
                    fm.getHeight()*2, fm.getHeight()*2);
            g.setColor(Color.BLACK);
            g.drawOval(
                    x - (int)rect.getWidth()/2,
                    y - fm.getHeight()-fm.getDescent(),
                    fm.getHeight()*2, fm.getHeight()*2);
            g.drawString(text,
                    x + (int)(rect.getHeight()-rect.getWidth()),
                    y + fm.getDescent());
        }
    }

    int nodeAt(Point p)
    {
        for (int i = 0; i < nodes.length; ++i) {
            Point2D.Float check = nodes[i].point;
            int x = toScreenX(check.x, getWidth());
            int y = toScreenY(check.y, getHeight());
            if (p.x >= x-radius && p.x < x+radius*2 &&
                p.y >= y-radius && p.y < y+radius*2) {
                return i;
            }
        }

        return -1;
    }
}

class StateNodeTableModel extends AbstractTableModel
{
    StateNode[] data;
    String[] names = {"Id", "Dist", "Cost", "Prev", "Seen"};

    StateNodeTableModel(StateNode data[])
    {
        this.data = data;
    }

    void setData(StateNode newdata[])
    {
        data = newdata;
        fireTableDataChanged();
    }

    public int getColumnCount()
    {
        return names.length;
    }

    public int getRowCount()
    {
        if (data == null)
            return 0;
        return data.length;
    }

    public String getColumnName(int col)
    {
        return names[col];
    }

    public Object getValueAt(int row, int col)
    {
        switch (col) {
        case 0: return ""+(char)(data[row].id+'A');
        case 1: return data[row].dist;
        case 2: return data[row].cost;
        case 3: return ""+(char)(data[row].prev+'A');
        case 4: return data[row].seen;
        }
        return null;
    }

    public Class getColumnClass(int col)
    {
        switch (col) {
        case 0: return String.class;
        case 1: return Integer.class;
        case 2: return Integer.class;
        case 3: return String.class;
        case 4: return Boolean.class;
        }
        return null;
    }

    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    public void setValueAt(Object value, int row, int col)
    {
    }
}

record GraphRecord(StateNode[] nodes, int currId, int nextId, StateNode[] queue)
{
}

class StateNode implements Comparable<StateNode>
{
    int id;
    int dist;
    int cost;
    int prev;
    boolean seen;

    StateNode(int id, int dist)
    {
        this.id = id;
        this.dist = dist;
    }

    StateNode(StateNode other)
    {
        this.id = other.id;
        this.dist = other.dist;
        this.cost = other.cost;
        this.prev = other.prev;
        this.seen = other.seen;
    }

    @Override
    public int compareTo(StateNode other)
    {
        return Integer.compare(dist+cost, other.dist+other.cost);
    }
}

class GraphView extends JPanel implements ChangeListener, ActionListener, PopupMenuListener, MouseListener
{
    Graph graph = new Graph(4, 4);
    ArrayList<GraphRecord> history;
    JTable table;
    TableColumn tableCostCol;
    JTable qtable;
    TableColumn qtableCostCol;
    JTextField currTime, maxTime;
    JSlider time;
    JComboBox<String> algo;
    int start, end;
    GraphView()
    {
        setLayout(new BorderLayout());
        graph.addMouseListener(this);

        end = graph.nodes.length-1;
        algo = new JComboBox<>(new String[]{"Dijkstra", "A*"});
        algo.setSelectedIndex(0);
        algo.addActionListener(this);
        algo.addPopupMenuListener(this);
        add(algo, BorderLayout.NORTH);

        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBackground(Color.WHITE);
        JPanel tables = new JPanel();
        table = new JTable(new StateNodeTableModel(null));
        JScrollPane scroll = new JScrollPane(table);
        qtable = new JTable(new StateNodeTableModel(null));
        JSplitPane split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scroll, new JScrollPane(qtable));
        split2.setContinuousLayout(true);
        split2.setResizeWeight(0.0);
        split2.setDividerLocation(scroll.getPreferredSize().height);
        tables.add(split2);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                graph, split2);
        split.setContinuousLayout(true);
        split.setResizeWeight(0.0);
        split.setDividerLocation(500);
        graphPanel.add(split);
        add(graphPanel, BorderLayout.CENTER);

        time = new JSlider(0, 0, 0);
        time.addChangeListener(this);
        time.setMajorTickSpacing(1);
        time.setPaintTicks(true);
        time.setPreferredSize(new Dimension(4000, 24));
        maxTime = new JTextField();
        currTime = new JTextField();
        currTime.setEditable(false);
        maxTime.setEditable(false);
        maxTime.setText(String.valueOf(0));
        currTime.setText(String.valueOf(0));
        JPanel timeline = new JPanel(new BorderLayout());
        timeline.add(currTime, BorderLayout.WEST);
        timeline.add(time, BorderLayout.CENTER);
        timeline.add(maxTime, BorderLayout.EAST);
        add(timeline, BorderLayout.SOUTH);

        tableCostCol = table.getColumnModel().getColumn(2);
        qtableCostCol = qtable.getColumnModel().getColumn(2);

        setAlgo(0);
    }

    private void setAlgo(int index)
    {
        table.removeColumn(tableCostCol);
        qtable.removeColumn(qtableCostCol);
        if (index == 0) {
            history = dijkstra(graph, start, end);
        } else if (index == 1) {
            history = astar(graph, start, end);
            table.addColumn(tableCostCol);
            qtable.addColumn(qtableCostCol);
        } else {
            throw new RuntimeException("Unknown algorithm");
        }
        time.setMaximum(history.size()-1);
        maxTime.setText(String.valueOf(history.size()-1));
        time.setValue(history.size()-1);
        revalidate();
        setFrame(history.get(time.getValue()));
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        graph.repaint();
    }
    public void popupMenuCanceled(PopupMenuEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JComboBox cb = (JComboBox)e.getSource();
        int index = cb.getSelectedIndex();
        setAlgo(index);
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        JSlider slider = (JSlider)e.getSource();
        int val = slider.getValue();
        currTime.setText(String.valueOf(val));
        revalidate();
        setFrame(history.get(val));
    }

    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e)
    {
        int i = graph.nodeAt(e.getPoint());
        if (i == -1)
            return;
        if (e.getButton() == 1) {
            start = i;
            setAlgo(algo.getSelectedIndex());
        } else if (e.getButton() == 3) {
            end = i;
            setAlgo(algo.getSelectedIndex());
        }
    }
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    private void setFrame(GraphRecord record)
    {
        GraphEdge e;
        StateNodeTableModel model = (StateNodeTableModel)table.getModel();
        model.setData(record.nodes());
        StateNodeTableModel qmodel = (StateNodeTableModel)qtable.getModel();
        qmodel.setData(record.queue());
        table.repaint();
        for (GraphEdge ed : graph.edges) {
            ed.color = Color.BLACK;
            ed.stroke = GraphEdge.THIN;
        }
        for (int row = 0; row < table.getRowCount(); ++row) {
            StateNode node = (StateNode)model.data[row];
            if (node.seen) {
                graph.nodes[node.id].color = Color.ORANGE;
            } else {
                graph.nodes[node.id].color = Color.WHITE;
            }
            if (node.id == record.nextId()) {
                graph.nodes[node.id].color = Color.CYAN;
            } else if (node.id == record.currId()) {
                graph.nodes[node.id].color = Color.GREEN;
            }
        }
        graph.nodes[start].color = Color.LIGHT_GRAY;
        graph.nodes[end].color = Color.MAGENTA;

        int it = record.currId();
        while (it != start) {
            e = graph.mat[it][record.nodes()[it].prev];
            if (e != null) {
                e.color = Color.ORANGE;
                e.stroke = GraphEdge.THICK;
            }
            it = record.nodes()[it].prev;
        }

        e = graph.mat[record.currId()][record.nextId()];
        if (e != null) {
            e.color = Color.CYAN;
            e.stroke = GraphEdge.THICK;
        }
        repaint();
    }

    private static void makeHistory(ArrayList<GraphRecord> history,
            StateNode[] record,
            int currId, int nextId,
            PriorityQueue<StateNode> queue)
    {
        StateNode[] clone = new StateNode[record.length];
        for (int i = 0; i < clone.length; ++i) {
            clone[i] = new StateNode(record[i]);
        }
        history.add(new GraphRecord(clone, currId, nextId, queue.toArray(new StateNode[queue.size()])));
    }

    private static ArrayList<GraphRecord> dijkstra(Graph graph, int beg, int end)
    {
        ArrayList<GraphRecord> history = new ArrayList<>();
        StateNode[] state = new StateNode[graph.nodes.length];
        for (int i = 0; i < state.length; ++i)
            state[i] = new StateNode(i, Integer.MAX_VALUE);

        PriorityQueue<StateNode> queue = new PriorityQueue<>();
        state[beg].dist = 0;
        queue.add(state[beg]);
        do {
            StateNode curr = queue.poll();

            if (curr.seen)
                continue;
            curr.seen = true;

            makeHistory(history, state, curr.id, curr.id, queue);

            if (curr.id == end)
                break;

            for (int n = 0; n < graph.mat[curr.id].length; ++n) {
                if (graph.mat[curr.id][n] == null)
                    continue;

                StateNode next = state[n];
                Point2D.Float p1 = graph.nodes[n].point;
                Point2D.Float p2 = graph.nodes[curr.id].point;
                int weight = (int)p1.distance(p2);

                if (next.seen)
                    continue;

                int newDist = curr.dist + weight;
                if (newDist < next.dist) {
                    next.dist = newDist;
                    next.prev = curr.id;
                    queue.add(next);
                }

                makeHistory(history, state, curr.id, next.id, queue);
            }
        } while (!queue.isEmpty());

        List<Integer> path = new ArrayList<>();
        int it = end;
        while (it != beg) {
            StateNode node = state[it];
            path.add(0, node.id);
            it = node.prev;
        }

        return history;
    }

    private static ArrayList<GraphRecord> astar(Graph graph, int beg, int end)
    {
        ArrayList<GraphRecord> history = new ArrayList<>();
        StateNode[] state = new StateNode[graph.nodes.length];
        for (int i = 0; i < state.length; ++i)
            state[i] = new StateNode(i, Integer.MAX_VALUE);

        PriorityQueue<StateNode> queue = new PriorityQueue<>();
        state[beg].dist = 0;
        queue.add(state[beg]);
        do {
            StateNode curr = queue.poll();

            if (curr.seen)
                continue;
            curr.seen = true;

            makeHistory(history, state, curr.id, curr.id, queue);

            if (curr.id == end)
                break;

            for (int n = 0; n < graph.mat[curr.id].length; ++n) {
                if (graph.mat[curr.id][n] == null)
                    continue;

                StateNode next = state[n];
                Point2D.Float pNext = graph.nodes[n].point;
                Point2D.Float pCurr = graph.nodes[curr.id].point;
                Point2D.Float pEnd = graph.nodes[end].point;
                //int cost = (int)Math.abs(pEnd.x - pNext.x + pEnd.y - pNext.y);
                int cost = (int)pEnd.distance(pNext);
                int weight = (int)pNext.distance(pCurr);

                if (next.seen)
                    continue;

                int newDist = curr.dist + weight;
                if (newDist < next.dist) {
                    next.dist = newDist;
                    next.cost = cost;
                    next.prev = curr.id;
                    queue.add(next);
                }

                makeHistory(history, state, curr.id, next.id, queue);
            }
        } while (!queue.isEmpty());

        List<Integer> path = new ArrayList<>();
        int it = end;
        while (it != beg) {
            StateNode node = state[it];
            path.add(0, node.id);
            it = node.prev;
        }

        return history;
    }
}

public class ShortestPath implements Runnable
{

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new ShortestPath());
    }

    @Override
    public void run()
    {
        JFrame frame;
        frame = new JFrame();
        frame.setTitle("Shortest Path");
        frame.setLayout(new BorderLayout());
        frame.setSize(800, 800);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.add(new GraphView());
        frame.setVisible(true);
    }
}
