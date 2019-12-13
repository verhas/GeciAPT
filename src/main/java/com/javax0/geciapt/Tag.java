package com.javax0.geciapt;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Tag {
    private final String tag;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Tag> subtags = new LinkedList<>();
    private final String cData;

    private Tag(final String tag, final String cData) {
        this.tag = tag;
        this.cData = cData;
    }

    public static Tag xml(final String tag) {
        return new Tag(tag, null);
    }

    public static Tag xml(final String tag, final String cdata) {
        return new Tag(tag, cdata);
    }

    public void attribute(final String name, final Object value) {
        if (value != null ) {
            if( attributes.containsKey(name)){
                final var e = new RuntimeException(name+" is double defined in "+tag);
                e.printStackTrace();
                throw e;
            }
            attributes.put(name, value.toString());
        }
    }

    public void subtag(final Tag tag) {
        if (tag != null) {
            subtags.add(tag);
        }
    }

    public boolean hasSubTags(){
        return subtags.size() > 0;
    }

    private void toStringBuilder(final StringBuilder sb, final int tab) {
        sb.append(" ".repeat(tab));
        sb.append("<").append(tag);
        for (final var e : attributes.entrySet()) {
            sb.append(" ").append(e.getKey()).append("=\"").append(e.getValue()).append("\"");
        }
        if (subtags.size() == 0 && cData == null) {
            sb.append("/>\n");
        } else {
            sb.append(">\n");
            for (final var subtag : subtags) {
                subtag.toStringBuilder(sb, tab + 4);
            }
            if (cData != null) {
                sb.append(" ".repeat(tab));
                sb.append("<![CDATA[").append(cData).append("]]>\n");
            }
            sb.append(" ".repeat(tab));
            sb.append("</").append(tag).append(">\n");
        }
    }

    public String toString() {
        final var sb = new StringBuilder();
        toStringBuilder(sb, 0);
        return sb.toString();
    }

}
