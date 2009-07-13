/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.management.ui.views;

import javax.management.MBeanServerConnection;

import static org.apache.qpid.management.ui.Constants.*;
import org.apache.qpid.management.ui.ApplicationRegistry;
import org.apache.qpid.management.ui.ManagedBean;
import org.apache.qpid.management.ui.ManagedServer;
import org.apache.qpid.management.ui.jmx.JMXManagedObject;
import org.apache.qpid.management.ui.jmx.JMXServerRegistry;
import org.apache.qpid.management.ui.jmx.MBeanUtility;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

/**
 * MBean View create appropriate view based on the user selection on the Navigation View.
 */
public class MBeanView extends ViewPart
{
    public static final String ID = "org.apache.qpid.management.ui.mbeanView";
    
    private FormToolkit  _toolkit = null;
    private Form _form = null;
    private String _formText = APPLICATION_NAME;
    private static ManagedServer _server = null;
    private TreeObject _selectedNode = null;
    private ManagedBean _mbean = null;
    private static String _virtualHostName = null;
    private static MBeanServerConnection _mbsc = null;
    private TabFolder _tabFolder = null;
    private ISelectionListener _selectionListener = new SelectionListenerImpl();

    // TabFolder to list all the mbeans for a given mbeantype(eg Connection, Queue, Exchange)
    private TabFolder _typeTabFolder = null;
    
    private TabFolder _notificationTabFolder = null;
    /*
     * Listener for the selection events in the navigation view
     */ 
    private class SelectionListenerImpl implements ISelectionListener
    {
        public void selectionChanged(IWorkbenchPart part, ISelection sel)
        {
            if (!(sel instanceof IStructuredSelection))
                return;

            IStructuredSelection ss = (IStructuredSelection) sel;
            _selectedNode = (TreeObject)ss.getFirstElement();
            
            
            // mbean should be set to null. A selection done on the navigation view can be either an mbean or
            // an mbeantype. For mbeantype selection(eg Connection, Queue, Exchange) _mbean will remain null.
            _mbean = null;
            setInvisible();
            
            // If a selected node(mbean) gets unregistered from mbean server, mbeanview should 
            // make the tabfolber for that mbean invisible
            if (_selectedNode == null)            
                return;
            
            setServer();
            refreshMBeanView();
            setFormTitle();            
        }
    }
    
    private void setFormTitle()
    {
        if (_mbean != null)
        {
            _formText = _mbean.getType();
            if ((_mbean.getVirtualHostName() != null) && (!DEFAULT_VH.equals(_mbean.getVirtualHostName())) )
            {
                _formText = _formText.replaceFirst(VIRTUAL_HOST, _mbean.getVirtualHostName());
                if (_mbean.getName() != null && _mbean.getName().length() != 0)
                {
                    _formText = _formText + ": " + _mbean.getName();
                }
            }
        }
        else if ((_selectedNode.getVirtualHost() != null) && (!DEFAULT_VH.equals(_selectedNode.getVirtualHost())))
        {
            _formText = _selectedNode.getVirtualHost();
        }
        else
        {
            _formText = APPLICATION_NAME;
        }
        _form.setText(_formText);
    }
    
    public void refreshMBeanView()
    {
        try
        {
            if (_selectedNode == null || NODE_TYPE_SERVER.equals(_selectedNode.getType()))
            {
                return;
            }
            else if (NODE_TYPE_TYPEINSTANCE.equals(_selectedNode.getType()))
            {
                // An virtual host instance is selected
                refreshTypeTabFolder(_typeTabFolder.getItem(0));
            }
            else if (NODE_TYPE_MBEANTYPE.equals(_selectedNode.getType()))
            {
                refreshTypeTabFolder(_selectedNode.getName());
            } 
            else if (NOTIFICATIONS.equals(_selectedNode.getType()))
            {
                refreshNotificationPage();
            }
            else if (MBEAN.equals(_selectedNode.getType()))
            {
                _mbean = (ManagedBean)_selectedNode.getManagedObject(); 
                showSelectedMBean();
            }
            
            _form.layout(true);
            _form.getBody().layout(true, true);
        }
        catch(Exception ex)
        {
            MBeanUtility.handleException(_mbean, ex);
        }
    }

    /**
     * Sets the managedServer based on the selection in the navigation view
     * At any given time MBeanView will be displaying information for an mbean of mbeantype
     * for a specifiv managed server. This server information will be used by the tab controllers
     * to get server registry.
     */
    private void setServer()
    {
        if (NODE_TYPE_SERVER.equals(_selectedNode.getType()))
        {
            _server = (ManagedServer)_selectedNode.getManagedObject();
            _virtualHostName = null;
        }
        else
        {
            TreeObject parent = _selectedNode.getParent();
            while (parent != null && !parent.getType().equals(NODE_TYPE_SERVER))
            {
                parent = parent.getParent();
            }
            
            if (parent != null && parent.getType().equals(NODE_TYPE_SERVER))
                _server = (ManagedServer)parent.getManagedObject();
            
            _virtualHostName = _selectedNode.getVirtualHost();
        }
        
        JMXServerRegistry serverRegistry = (JMXServerRegistry)ApplicationRegistry.getServerRegistry(_server);
        if(serverRegistry != null){
            _mbsc = serverRegistry.getServerConnection();
        }
    }
    
    public static ManagedServer getServer()
    {
        return _server;
    }
    
    public static String getVirtualHost()
    {
        return _virtualHostName;
    }
    
    private void showSelectedMBean() throws Exception
    {           
        try
        {                
            MBeanUtility.getMBeanInfo(_mbean);     
        }
        catch(Exception ex)
        {
            MBeanUtility.handleException(_mbean, ex);
            return;
        }

        if (_tabFolder != null && !_tabFolder.isDisposed())
        {
            _tabFolder.dispose();
        }
        
        _tabFolder = MBeanTabFolderFactory.generateMBeanTabFolder(_form.getBody(),(JMXManagedObject)_mbean,_mbsc);
        
        int tabIndex = 0;
        if (NOTIFICATIONS.equals(_selectedNode.getType()))
        {
            tabIndex = _tabFolder.getItemCount() -1;
        }
       
        TabItem tab = _tabFolder.getItem(tabIndex);
        // If folder is being set as visible after tab refresh, then the tab 
        // doesn't have the focus.                  
        _tabFolder.setSelection(tabIndex);
        refreshTab(tab);
    }
    
    public void createPartControl(Composite parent)
    {
        // Create the Form
        _toolkit = new FormToolkit(parent.getDisplay());
        _form = _toolkit.createForm(parent);
        _form.getBody().setLayout(new FormLayout());
        _form.setText(APPLICATION_NAME);
        
        // Add selection listener for selection events in the Navigation view
        getSite().getPage().addSelectionListener(NavigationView.ID, _selectionListener); 
        
        // Add mbeantype TabFolder. This will list all the mbeans under a mbeantype (eg Queue, Exchange).
        // Using this list mbeans will be added in the navigation view
        createMBeanTypeTabFolder();
        
        createNotificationsTabFolder();
    }
    
    private void refreshTab(TabItem tab)
    {
        if (tab == null)
        {
            return;
        }
        
        TabControl controller = (TabControl)tab.getData(TabControl.CONTROLLER);
        if(controller != null){
            controller.refresh(_mbean);
        }
    }
    
    public void setFocus()
    {   
        //_form.setFocus();
    }

    public void dispose()
    {
        _toolkit.dispose();
        super.dispose();
    }
    
    /**
     * Creates TabFolder and tabs for each mbeantype (eg Connection, Queue, Exchange)
     */
    private void createMBeanTypeTabFolder()
    {
        _typeTabFolder = new TabFolder(_form.getBody(), SWT.NONE);
        FormData layoutData = new FormData();
        layoutData.left = new FormAttachment(0);
        layoutData.top = new FormAttachment(0);
        layoutData.right = new FormAttachment(100);
        layoutData.bottom = new FormAttachment(100);
        _typeTabFolder.setLayoutData(layoutData);
        _typeTabFolder.setVisible(false);
              
        TabItem tab = new TabItem(_typeTabFolder, SWT.NONE);
        tab.setText(CONNECTION); 
        MBeanTypeTabControl controller = new ConnectionTypeTabControl(_typeTabFolder);
        tab.setData(TabControl.CONTROLLER, controller);
        tab.setControl(controller.getControl());
        
        tab = new TabItem(_typeTabFolder, SWT.NONE);
        tab.setText(EXCHANGE);      
        controller = new ExchangeTypeTabControl(_typeTabFolder);
        tab.setData(TabControl.CONTROLLER, controller);
        tab.setControl(controller.getControl());
        
        tab = new TabItem(_typeTabFolder, SWT.NONE);
        tab.setText(QUEUE);  
        controller = new QueueTypeTabControl(_typeTabFolder);
        tab.setData(TabControl.CONTROLLER, controller);
        tab.setControl(controller.getControl());
        
        _typeTabFolder.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event evt)
            {
                TabItem tab = (TabItem)evt.item;     
                try
                {
                    refreshTypeTabFolder(tab);
                }
                catch (Exception ex)
                {
                    MBeanUtility.handleException(ex);
                }
            }
        });
    }
    
    private void createNotificationsTabFolder()
    {
        _notificationTabFolder = new TabFolder(_form.getBody(), SWT.NONE);
        FormData layoutData = new FormData();
        layoutData.left = new FormAttachment(0);
        layoutData.top = new FormAttachment(0);
        layoutData.right = new FormAttachment(100);
        layoutData.bottom = new FormAttachment(100);
        _notificationTabFolder.setLayoutData(layoutData);
        _notificationTabFolder.setVisible(false);
        
        VHNotificationsTabControl controller = new VHNotificationsTabControl(_notificationTabFolder);       
        TabItem tab = new TabItem(_notificationTabFolder, SWT.NONE);
        tab.setText(NOTIFICATIONS);
        tab.setData(TabControl.CONTROLLER, controller);
        tab.setControl(controller.getControl());
    }
    
    private void refreshNotificationPage()
    {        
        TabItem tab = _notificationTabFolder.getItem(0);
        VHNotificationsTabControl controller = (VHNotificationsTabControl)tab.getData(TabControl.CONTROLLER);
        controller.refresh();
        _notificationTabFolder.setVisible(true);
    }
    
    /**
     * Refreshes the Selected mbeantype tab. The control lists all the available mbeans
     * for an mbeantype(eg Queue, Exchange etc)
     * @param tab
     * @throws Exception
     */
    private void refreshTypeTabFolder(TabItem tab) throws Exception
    {
        if (tab == null)
        {
            return;
        }
        _typeTabFolder.setSelection(tab);
        MBeanTypeTabControl controller = (MBeanTypeTabControl)tab.getData(TabControl.CONTROLLER);
        controller.refresh();
        _typeTabFolder.setVisible(true);
    }
    
    private void refreshTypeTabFolder(String type) throws Exception
    {
        if (CONNECTION.equals(type))
        {
            refreshTypeTabFolder(_typeTabFolder.getItem(0));
        }
        else if (EXCHANGE.equals(type))
        {
            refreshTypeTabFolder(_typeTabFolder.getItem(1));
        }
        else if (QUEUE.equals(type))
        {
            refreshTypeTabFolder(_typeTabFolder.getItem(2));
        }
    }

    private void setInvisible()
    {
        if (_tabFolder != null)
        {
            _tabFolder.setVisible(false);
        }
        
        if (_typeTabFolder != null)
        {
            _typeTabFolder.setVisible(false);
        }
        
        if (_notificationTabFolder != null)
        {
            _notificationTabFolder.setVisible(false);
        }
    }
    
}
