package com.szas.server.gwt.client.universalwidgets;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.szas.sync.DAOObserver;
import com.szas.sync.Tuple;
import com.szas.sync.local.LocalDAO;

public abstract class UniversalList<T extends Tuple> extends Composite {
	private static class UniversalProvidesKey<TP extends Tuple> implements ProvidesKey<TP> {

		@Override
		public Object getKey(TP item) {
			return (item == null) ? null : item.getId();
		}

	}

	private SingleSelectionModel<T> selectionModel;
	private ProvidesKey<T> providesKey;
	private List<T> list;
	private DAOObserver contentObserver;
	private CellTable<T> cellTable;

	protected SingleSelectionModel<T> createSelectionModel() {
		SingleSelectionModel<T> singleSelectionModel = 
			new SingleSelectionModel<T>(providesKey);
		singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				changeSellection();
			}
		});
		return singleSelectionModel;
	}
	protected ProvidesKey<T> createProvidesKey() {
		return new UniversalProvidesKey<T>();
	}

	protected abstract void addColumns(CellTable<T> cellTable2);

	protected CellTable<T> createTable() {
		providesKey = createProvidesKey();
		cellTable = new CellTable<T>(providesKey);
		addColumns(cellTable);

		selectionModel = createSelectionModel();
		cellTable.setSelectionModel(selectionModel);

		createDataProvider(cellTable);
		return cellTable;
	}
	private ListDataProvider<T> createDataProvider(HasData<T> cellTable) {
		ListDataProvider<T> listDataProvider = new ListDataProvider<T>();
		listDataProvider.addDataDisplay(cellTable);
		list =listDataProvider.getList();
		return listDataProvider;
	}
	protected void changeSellection() {
		T tuple = selectionModel.getSelectedObject();
		if (tuple == null)
			return;
		History.newItem(getListName()+"," + tuple.getId(),true);
	}
	
	@Override
	protected void onAttach() {
		super.onAttach();
		contentObserver = new DAOObserver() {

			@Override
			public void onChange(boolean whileSync) {
				daoUpdated();
			}
		};
		getLocalDAO().addDAOObserver(contentObserver);
	}
	
	protected abstract LocalDAO<T> getLocalDAO();
	
	protected abstract String getListName();
	
	protected void daoUpdated() {
		Collection<T> tuples = getLocalDAO().getAll();
		cellTable.setRowCount(tuples.size(), true);
		while (list.size() != 0)
			list.remove(0);
		for (T tuple : tuples) {
			list.add(tuple);
		}
	}

	@Override
	protected void onDetach() {
		if (contentObserver != null)
			getLocalDAO().removeDAOObserver(contentObserver);
		contentObserver = null;
		super.onDetach();
	}
	
	protected void addButtonClicked() {
		History.newItem(getListName(),true);
	}
}
