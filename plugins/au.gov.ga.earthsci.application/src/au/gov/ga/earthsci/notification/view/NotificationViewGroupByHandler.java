package au.gov.ga.earthsci.notification.view;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

import au.gov.ga.earthsci.notification.view.NotificationView.Grouping;

/**
 * A handler that receives group-by events from the {@link NotificationView} menu
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
@Singleton
@Creatable
public class NotificationViewGroupByHandler
{
	public static final String ID = "au.gov.ga.earthsci.notification.view.groupby"; //$NON-NLS-1$
	
	public static final String GROUP_BY_NONE_MENU_ID = "au.gov.ga.earthsci.notification.view.NotificationView.group.none"; //$NON-NLS-1$
	public static final String GROUP_BY_LEVEL_MENU_ID = "au.gov.ga.earthsci.notification.view.NotificationView.group.level"; //$NON-NLS-1$
	public static final String GROUP_BY_CATEGORY_MENU_ID = "au.gov.ga.earthsci.notification.view.NotificationView.group.category"; //$NON-NLS-1$

	private static final Map<String, Grouping> idToGrouping = new HashMap<String, Grouping>();
	static
	{
		idToGrouping.put(GROUP_BY_NONE_MENU_ID, Grouping.NONE);
		idToGrouping.put(GROUP_BY_LEVEL_MENU_ID, Grouping.LEVEL);
		idToGrouping.put(GROUP_BY_CATEGORY_MENU_ID, Grouping.CATEGORY);
	}
	
	private NotificationView view;

	public void setView(NotificationView view)
	{
		this.view = view;
	}
	
	@Execute
	public void execute(MMenuItem item)
	{
		if (!item.isSelected())
		{
			return;
		}
		view.setGrouping(idToGrouping.get(item.getElementId()));
	}

}
