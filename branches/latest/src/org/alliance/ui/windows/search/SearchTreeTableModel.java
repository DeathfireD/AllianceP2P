package org.alliance.ui.windows.search;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.PacedRunner;
import org.alliance.core.comm.SearchHit;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-24
 * Time: 14:13:14
 */
public class SearchTreeTableModel extends DefaultTreeTableModel {

    private CoreSubsystem core;

    public SearchTreeTableModel(CoreSubsystem core, PacedRunner pacedRunner) {
        super(new RootNode());
        this.core = core;
        getRoot().setModel(this, pacedRunner);
    }

    @Override
    public RootNode getRoot() {
        return (RootNode) super.getRoot();
    }

    public void addSearchHits(int sourceGuid, int hops, java.util.List<SearchHit> hits) {
        getRoot().addSearchHits(sourceGuid, hops, hits);
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 1) {
            return Long.class;
        }
        if (columnIndex == 2) {
            return String.class;
        }
        if (columnIndex == 3) {
            return Integer.class;
        }
        if (columnIndex == 4) {
            return Double.class;
        }
        if (columnIndex == 5) {
            return Double.class;
        }
        return super.getColumnClass(columnIndex);
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "File";
            case 1:
                return "Size";
            case 2:
                return "Type";
            case 3:
                return "Days ago";
            case 4:
                return "#";
            case 5:
                return "Speed";
            default:
                return "undefined";
        }
    }

    @Override
    public Object getValueAt(Object o, int i) {
        SearchTreeNode n = (SearchTreeNode) o;
        return n.getValueAt(i);
    }

    Comparator<SearchTreeNode> createNameComparator() {
        return new Comparator<SearchTreeNode>() {

            @Override
            public int compare(SearchTreeNode o1, SearchTreeNode o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
    }

    Comparator<SearchTreeNode> createSourcesComparator() {
        return new Comparator<SearchTreeNode>() {

            @Override
            public int compare(SearchTreeNode o1, SearchTreeNode o2) {
                return (int) Math.round((o2.getSources() - o1.getSources()) * 1000);
            }
        };
    }

    Comparator<SearchTreeNode> createDaysAgoComparator() {
        return new Comparator<SearchTreeNode>() {

            @Override
            public int compare(SearchTreeNode o1, SearchTreeNode o2) {
                return (int) Math.round((o1.getDaysAgo() - o2.getDaysAgo()) * 1000);
            }
        };
    }

    Comparator<SearchTreeNode> createSizeComparator() {
        return new Comparator<SearchTreeNode>() {

            @Override
            public int compare(SearchTreeNode o1, SearchTreeNode o2) {
                return (int) (o2.getSize() - o1.getSize());
            }
        };
    }

    Comparator<SearchTreeNode> createSpeedComparator() {
        return new Comparator<SearchTreeNode>() {

            @Override
            public int compare(SearchTreeNode o1, SearchTreeNode o2) {
                return (int) Math.round((o2.getSpeed() - o1.getSpeed()) * 1000);
            }
        };
    }

    public CoreSubsystem getCore() {
        return core;
    }
}
