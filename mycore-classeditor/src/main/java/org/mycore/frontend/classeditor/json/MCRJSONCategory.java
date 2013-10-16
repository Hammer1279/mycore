package org.mycore.frontend.classeditor.json;

import java.util.List;
import java.util.Set;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

/**
 * GSON Category abstraction.
 * 
 * @author Chi
 */
public class MCRJSONCategory implements MCRCategory {
    private MCRCategoryImpl category;

    private Boolean hasChildren = null;

    public void setParent(MCRCategory parent) {
        category.setParent(parent);
    }

    public void setChildren(List<MCRCategory> children) {
        category.setChildren(children);
    }

    public MCRJSONCategory() {
        category = new MCRCategoryImpl();
    }

    public MCRJSONCategory(MCRCategory category) {
        this.category = (MCRCategoryImpl) category;
    }

    public int getLeft() {
        return category.getLeft();
    }

    public int getLevel() {
        return category.getLevel();
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public boolean hasChildren() {
        if (hasChildren == null) {
            hasChildren = category.hasChildren();
        }
        return hasChildren;
    }

    public List<MCRCategory> getChildren() {
        return category.getChildren();
    }

    public int getPositionInParent() {
        return category.getPositionInParent();
    }

    public MCRCategoryID getId() {
        return category.getId();
    }

    public Set<MCRLabel> getLabels() {
        return category.getLabels();
    }

    public MCRCategory getRoot() {
        return category.getRoot();
    }

    public java.net.URI getURI() {
        return category.getURI();
    }

    public void setId(MCRCategoryID id) {
        category.setId(id);
    }

    public void setURI(java.net.URI uri) {
        category.setURI(uri);
    }

    public MCRCategory getParent() {
        return category.getParent();
    }

    public MCRLabel getCurrentLabel() {
        return category.getCurrentLabel();
    }

    public void setLabels(Set<MCRLabel> labels) {
        category.setLabels(labels);
    }

    public MCRLabel getLabel(String lang) {
        return category.getLabel(lang);
    }

    public void setPositionInParent(int positionInParent) {
        category.setPositionInParent(positionInParent);
    }

    private MCRCategoryID parentID;

    public void setParentID(MCRCategoryID parentID) {
        this.parentID = parentID;
    }

    public MCRCategoryID getParentID() {
        return parentID;
    }

    @Override
    public boolean isClassification() {
        return category.isClassification();
    }

    @Override
    public boolean isCategory() {
        return category.isCategory();
    }

    public MCRCategoryImpl asMCRImpl() {
        return category;
    }

}