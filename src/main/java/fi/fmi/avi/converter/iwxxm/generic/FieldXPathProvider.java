package fi.fmi.avi.converter.iwxxm.generic;

import java.util.List;

public interface FieldXPathProvider {
    List<String> getXPaths(IWXXMField field);
}

