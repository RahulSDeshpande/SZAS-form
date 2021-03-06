package com.szas.server.gwt.client.universalwidgets;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.szas.sync.Tuple;
import com.szas.sync.local.LocalDAO;

public abstract class UniversalWidget<T extends Tuple> extends Composite {
	protected T tuple;
	protected boolean update;
	
	protected abstract void initWidget();
	protected abstract LocalDAO<T> getLocalDAO();
	protected abstract void updateTuple();
	protected abstract void setDeleteable(boolean deletable);
	
	public UniversalWidget(T tuple) {
		this.tuple = tuple;
	}
	
	@Override
	protected void onAttach() {
		super.onAttach();
		update = getLocalDAO().getById(tuple.getId()) != null;
		setDeleteable(update);
		updateWidgets();
	}
	
	protected abstract void updateWidgets();
	
	protected void onSave() {
		updateTuple();
		if (update)
			getLocalDAO().update(tuple);
		else
			getLocalDAO().insert(tuple);
		History.back();
	}
	
	protected void onDelete() {
		if (!update)
			return;
		getLocalDAO().delete(tuple);
		History.back();
	}

}
