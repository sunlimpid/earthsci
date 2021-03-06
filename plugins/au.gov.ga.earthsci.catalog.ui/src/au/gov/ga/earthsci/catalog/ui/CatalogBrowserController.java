/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.earthsci.catalog.ui;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;

import au.gov.ga.earthsci.application.util.UserActionPreference;
import au.gov.ga.earthsci.catalog.CatalogLayerHelper;
import au.gov.ga.earthsci.catalog.ICatalogTreeNode;
import au.gov.ga.earthsci.catalog.ui.preferences.ICatalogBrowserPreferences;
import au.gov.ga.earthsci.core.model.layer.FolderNode;
import au.gov.ga.earthsci.core.model.layer.ILayerTreeNode;
import au.gov.ga.earthsci.core.model.layer.LayerNode;
import au.gov.ga.earthsci.core.worldwind.ITreeModel;

/**
 * The default implementation of the {@link ICatalogBrowserController}
 * interface.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
@Creatable
@Singleton
public class CatalogBrowserController implements ICatalogBrowserController
{
	@Inject
	private ITreeModel currentLayerModel;

	@Inject
	private ICatalogBrowserPreferences preferences;

	@Inject
	private IEclipseContext context;

	@Override
	public boolean existsInLayerModel(URI layerURI)
	{
		return currentLayerModel.getRootNode().hasNodesForURI(layerURI);
	}

	@Override
	public boolean allExistInLayerModel(ICatalogTreeNode... nodes)
	{
		if (nodes == null || nodes.length == 0)
		{
			return true;
		}
		boolean allExistInModel = true;
		for (ICatalogTreeNode n : nodes)
		{
			if (n == null)
			{
				continue;
			}
			if (n.isLayerNode())
			{
				allExistInModel = allExistInModel && existsInLayerModel(n.getLayerURI());
			}
			if (n.getChildCount() > 0)
			{
				allExistInModel =
						allExistInModel
								&& allExistInLayerModel(n.getChildren()
										.toArray(new ICatalogTreeNode[n.getChildCount()]));
			}
			if (!allExistInModel)
			{
				return false;
			}
		}
		return allExistInModel;
	}

	@Override
	public boolean anyExistInLayerModel(ICatalogTreeNode... nodes)
	{
		if (nodes == null || nodes.length == 0)
		{
			return false;
		}
		boolean anyExistInModel = false;
		for (ICatalogTreeNode n : nodes)
		{
			if (n == null)
			{
				continue;
			}
			if (n != null && n.isLayerNode())
			{
				anyExistInModel = anyExistInModel || existsInLayerModel(n.getLayerURI());
			}
			if (n.getChildCount() > 0)
			{
				anyExistInModel =
						anyExistInModel
								|| anyExistInLayerModel(n.getChildren()
										.toArray(new ICatalogTreeNode[n.getChildCount()]));
			}
			if (anyExistInModel)
			{
				return true;
			}
		}
		return anyExistInModel;
	}

	@Override
	public boolean areAllLayerNodes(ICatalogTreeNode... nodes)
	{
		if (nodes == null)
		{
			return true;
		}
		for (ICatalogTreeNode node : nodes)
		{
			if (node != null && !node.isLayerNode())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public void addToLayerModel(ICatalogTreeNode... nodes)
	{
		boolean fullNodePathRequiredOnAdd = isFullNodePathRequiredOnAdd();
		for (ICatalogTreeNode node : nodes)
		{
			ILayerTreeNode parent = fullNodePathRequiredOnAdd ? createNodePath(node) : currentLayerModel.getRootNode();
			CatalogLayerHelper.insertIntoLayerModel(parent, node, context);
		}
	}

	/**
	 * Initialises (if required) the node path in the current layer model into
	 * which the given node and its children should be inserted. The parent node
	 * into which the given catalog node should be inserted is returned.
	 * <p/>
	 * If the node path does not yet exist, it will be created.
	 * <p/>
	 * If the node path does exist, it will be reused.
	 * 
	 * @param node
	 *            The catalog being inserted into the layer model
	 * 
	 * @return The node into which the given catalog node (and its children)
	 *         should be inserted. This may be an existing node, or a new node
	 *         as required.
	 */
	private ILayerTreeNode createNodePath(ICatalogTreeNode node)
	{
		if (node.isRoot())
		{
			return currentLayerModel.getRootNode();
		}

		ILayerTreeNode[] folders = currentLayerModel.getRootNode().getNodesForURI(node.getURI());
		if (folders.length != 0)
		{
			return folders[0];
		}

		ILayerTreeNode parent = createNodePath(node.getParent());

		if (!node.isLayerNode())
		{
			ILayerTreeNode folder = CatalogLayerHelper.createFolderNode(node);
			parent.addChild(folder);
			return folder;
		}

		return parent;
	}

	private boolean isFullNodePathRequiredOnAdd()
	{
		if (preferences.getAddNodeStructureMode() != UserActionPreference.ASK)
		{
			return preferences.getAddNodeStructureMode() == UserActionPreference.ALWAYS;
		}

		MessageDialogWithToggle message =
				MessageDialogWithToggle.openYesNoQuestion(null,
						Messages.CatalogBrowserController_AddNodePathDialogTitle,
						Messages.CatalogBrowserController_AddNodePathDialogMessage,
						Messages.CatalogBrowserController_DialogDontAsk, false, null, null);

		UserActionPreference preference =
				(message.getReturnCode() == IDialogConstants.YES_ID) ? UserActionPreference.ALWAYS
						: UserActionPreference.NEVER;
		preferences.setAddNodeStructureMode(message.getToggleState() ? preference : UserActionPreference.ASK);

		return preference == UserActionPreference.ALWAYS;
	}

	@Override
	public void removeFromLayerModel(ICatalogTreeNode... nodes)
	{
		Boolean deleteEmptyFolders = null;
		for (ICatalogTreeNode node : nodes)
		{
			if (node.isLayerNode())
			{
				for (ILayerTreeNode layer : currentLayerModel.getRootNode().getNodesForURI(node.getLayerURI()))
				{
					ILayerTreeNode parent = layer.getParent();
					layer.removeFromParent();

					// If the removal will create an empty folder, determine whether to remove it
					if ((parent instanceof FolderNode) && !parent.hasChildren())
					{
						if (deleteEmptyFolders == null)
						{
							deleteEmptyFolders = isEmptyFolderDeletionRequiredOnRemoval();
						}
						if (deleteEmptyFolders)
						{
							deleteEmptyFolders(parent);
						}
					}
				}
			}
		}
	}

	private void deleteEmptyFolders(ILayerTreeNode node)
	{
		if (node.isRoot())
		{
			return;
		}
		if (node.getParent().getChildCount() > 1 || (node.getParent() instanceof LayerNode))
		{
			node.removeFromParent();
			return;
		}
		deleteEmptyFolders(node.getParent());
	}

	private boolean isEmptyFolderDeletionRequiredOnRemoval()
	{
		if (preferences.getDeleteEmptyFoldersMode() != UserActionPreference.ASK)
		{
			return preferences.getDeleteEmptyFoldersMode() == UserActionPreference.ALWAYS;
		}

		MessageDialogWithToggle message =
				MessageDialogWithToggle.openYesNoQuestion(null,
						Messages.CatalogBrowserController_DeleteEmptyFoldersDialogTitle,
						Messages.CatalogBrowserController_DeleteEmptyFoldersMessage,
						Messages.CatalogBrowserController_DialogDontAsk, false, null, null);

		UserActionPreference preference =
				(message.getReturnCode() == IDialogConstants.YES_ID) ? UserActionPreference.ALWAYS
						: UserActionPreference.NEVER;
		preferences.setDeleteEmptyFoldersMode(message.getToggleState() ? preference : UserActionPreference.ASK);

		return preference == UserActionPreference.ALWAYS;
	}

	public void setCurrentLayerModel(ITreeModel currentLayerModel)
	{
		this.currentLayerModel = currentLayerModel;
	}
}
