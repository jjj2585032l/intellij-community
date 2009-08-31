/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.facet.*;
import com.intellij.facet.impl.ProjectFacetsConfigurator;
import com.intellij.facet.impl.ui.FacetEditorFacade;
import com.intellij.facet.impl.ui.FacetTreeModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.NamedConfigurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * @author nik
 */
public class FacetEditorFacadeImpl implements FacetEditorFacade {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.ui.configuration.projectRoot.FacetEditorFacadeImpl");
  private final ModuleStructureConfigurable myStructureConfigurable;
  private final Runnable myTreeUpdater;
  private final Map<Facet, MasterDetailsComponent.MyNode> myNodes = new HashMap<Facet, MasterDetailsComponent.MyNode>();
  private final Map<Facet, FacetConfigurable> myConfigurables = new HashMap<Facet, FacetConfigurable>();

  public FacetEditorFacadeImpl(final ModuleStructureConfigurable structureConfigurable, final Runnable treeUpdater) {
    myStructureConfigurable = structureConfigurable;
    myTreeUpdater = treeUpdater;
  }

  public boolean addFacetsNodes(final Module module, final MasterDetailsComponent.MyNode moduleNode) {
    boolean facetsExist = false;

    getFacetConfigurator().addFacetInfos(module);

    final FacetModel facetModel = getFacetConfigurator().getFacetModel(module);
    for (Facet facet : facetModel.getSortedFacets()) {
      addFacetNode(facet, moduleNode);
      facetsExist = true;
    }

    return facetsExist;
  }

  private MasterDetailsComponent.MyNode addFacetNode(final Facet facet, final MasterDetailsComponent.MyNode moduleNode) {
    final MasterDetailsComponent.MyNode existing = findFacetNode(facet, moduleNode);
    if (existing != null) return existing;

    final FacetConfigurable facetConfigurable = getOrCreateConfigurable(facet);
    final MasterDetailsComponent.MyNode facetNode = new MasterDetailsComponent.MyNode(facetConfigurable);
    myNodes.put(facet, facetNode);
    MasterDetailsComponent.MyNode parent = moduleNode;
    final Facet underlyingFacet = facet.getUnderlyingFacet();
    if (underlyingFacet != null) {
      parent = myNodes.get(underlyingFacet);
      LOG.assertTrue(parent != null);
    }
    myStructureConfigurable.addNode(facetNode, parent);
    return facetNode;
  }

  public FacetConfigurable getOrCreateConfigurable(final Facet facet) {
    FacetConfigurable configurable = myConfigurables.get(facet);
    if (configurable == null) {
      configurable = new FacetConfigurable(facet, getFacetConfigurator(), myTreeUpdater);
      myConfigurables.put(facet, configurable);
    }
    return configurable;
  }

  @Nullable
  private static MasterDetailsComponent.MyNode findFacetNode(final Facet facet, final MasterDetailsComponent.MyNode moduleNode) {
    for (int i = 0; i < moduleNode.getChildCount(); i++) {
      final TreeNode node = moduleNode.getChildAt(i);
      if (node instanceof MasterDetailsComponent.MyNode) {
        final MasterDetailsComponent.MyNode configNode = (MasterDetailsComponent.MyNode)node;
        final NamedConfigurable config = configNode.getConfigurable();
        if (config instanceof FacetConfigurable) {
          final Facet existingFacet = ((FacetConfigurable)config).getEditableObject();
          if (existingFacet != null && existingFacet.equals(facet)) {
            return configNode;
          }
        }
      }
    }

    return null;
  }

  public boolean nodeHasFacetOfType(final FacetInfo facet, FacetTypeId typeId) {
    final Module selectedModule = getSelectedModule();
    if (selectedModule == null) {
      return false;
    }
    final FacetTreeModel facetTreeModel = getFacetConfigurator().getTreeModel(selectedModule);
    return facetTreeModel.hasFacetOfType(facet, typeId);
  }

  public void createFacet(final FacetInfo parent, FacetType type, final String name) {
    Module module = getSelectedModule();

    final Facet facet = getFacetConfigurator().createAndAddFacet(module, type, name, parent);
    final MasterDetailsComponent.MyNode node = addFacetNode(facet, myStructureConfigurable.findModuleNode(module));
    myStructureConfigurable.selectNodeInTree(node);
  }

  public Collection<FacetInfo> getFacetsByType(final FacetType<?,?> type) {
    final Module selectedModule = getSelectedModule();
    if (selectedModule == null) return Collections.emptyList();
    final FacetModel facetModel = getFacetConfigurator().getFacetModel(selectedModule);
    final Collection<? extends Facet> facets = facetModel.getFacetsByType(type.getId());

    final ArrayList<FacetInfo> infos = new ArrayList<FacetInfo>();
    for (Facet facet : facets) {
      final FacetInfo facetInfo = getFacetConfigurator().getFacetInfo(facet);
      if (facetInfo != null) {
        infos.add(facetInfo);
      }
    }
    return infos;
  }

  @Nullable
  public FacetInfo getParent(final FacetInfo facetInfo) {
    final Module module = getFacetConfigurator().getFacet(facetInfo).getModule();
    return getFacetConfigurator().getTreeModel(module).getParent(facetInfo);
  }

  private ProjectFacetsConfigurator getFacetConfigurator() {
    return myStructureConfigurable.getFacetConfigurator();
  }

  @Nullable
  private Facet getSelectedFacet() {
    final Object selectedObject = myStructureConfigurable.getSelectedObject();
    if (selectedObject instanceof Facet) {
      return (Facet)selectedObject;
    }
    return null;
  }

  @Nullable
  private Module getSelectedModule() {
    final Object selected = myStructureConfigurable.getSelectedObject();
    if (selected instanceof Module) {
      return (Module)selected;
    }
    if (selected instanceof Facet) {
      return ((Facet)selected).getModule();
    }
    return null;
  }

  @Nullable
  public ModuleType getSelectedModuleType() {
    final Module module = getSelectedModule();
    return module != null ? module.getModuleType() : null;
  }

  @Nullable
  public FacetInfo getSelectedFacetInfo() {
    final Facet facet = getSelectedFacet();
    return facet != null ? getFacetConfigurator().getFacetInfo(facet) : null;
  }

  public void clearMaps() {
    myConfigurables.clear();
    myNodes.clear();
  }
}
