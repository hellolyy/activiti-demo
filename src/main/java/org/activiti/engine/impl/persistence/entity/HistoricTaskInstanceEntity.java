/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.persistence.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.impl.util.ClockUtil;

/**
 * @author Tom Baeyens
 */
public class HistoricTaskInstanceEntity extends HistoricScopeInstanceEntity implements HistoricTaskInstance, PersistentObject {

	private static final long serialVersionUID = 1L;

	protected String executionId;
	protected String name;
	protected String parentTaskId;
	protected String description;
	protected String owner;
	protected String assignee;
	protected String taskDefinitionKey;
	protected String formKey;
	protected int priority;
	protected Date dueDate;
	protected Date claimTime;
	protected List<HistoricVariableInstanceEntity> queryVariables;

	public HistoricTaskInstanceEntity() {
	}

	public HistoricTaskInstanceEntity(TaskEntity task, ExecutionEntity execution) {
		this.id = task.getId();
		if (execution != null) {
			this.processDefinitionId = execution.getProcessDefinitionId();
			this.processInstanceId = execution.getProcessInstanceId();
			this.executionId = execution.getId();
		}
		this.name = task.getName();
		this.parentTaskId = task.getParentTaskId();
		this.description = task.getDescription();
		this.owner = task.getOwner();
		this.assignee = task.getAssignee();
		this.startTime = ClockUtil.getCurrentTime();
		this.taskDefinitionKey = task.getTaskDefinitionKey();

		this.setPriority(task.getPriority());
		this.setDueDate(task.getDueDate());
	}

	// persistence
	// //////////////////////////////////////////////////////////////

	public Object getPersistentState() {
		Map<String, Object> persistentState = new HashMap<String, Object>();
		persistentState.put("name", name);
		persistentState.put("owner", owner);
		persistentState.put("assignee", assignee);
		persistentState.put("endTime", endTime);
		persistentState.put("durationInMillis", durationInMillis);
		persistentState.put("description", description);
		persistentState.put("deleteReason", deleteReason);
		persistentState.put("taskDefinitionKey", taskDefinitionKey);
		persistentState.put("formKey", formKey);
		persistentState.put("priority", priority);
		if (parentTaskId != null) {
			persistentState.put("parentTaskId", parentTaskId);
		}
		if (dueDate != null) {
			persistentState.put("dueDate", dueDate);
		}
		if (claimTime != null) {
			persistentState.put("claimTime", claimTime);
		}
		return persistentState;
	}

	// getters and setters
	// //////////////////////////////////////////////////////
	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public String getTaskDefinitionKey() {
		return taskDefinitionKey;
	}

	public void setTaskDefinitionKey(String taskDefinitionKey) {
		this.taskDefinitionKey = taskDefinitionKey;
	}

	public String getFormKey() {
		return formKey;
	}

	public void setFormKey(String formKey) {
		this.formKey = formKey;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getParentTaskId() {
		return parentTaskId;
	}

	public void setParentTaskId(String parentTaskId) {
		this.parentTaskId = parentTaskId;
	}

	public Date getClaimTime() {
		return claimTime;
	}

	public void setClaimTime(Date claimTime) {
		this.claimTime = claimTime;
	}

	public Long getWorkTimeInMillis() {
		if (endTime == null || claimTime == null) {
			return null;
		}
		return endTime.getTime() - claimTime.getTime();
	}

	public Map<String, Object> getTaskLocalVariables() {
		Map<String, Object> variables = new HashMap<String, Object>();
		if (queryVariables != null) {
			for (HistoricVariableInstanceEntity variableInstance : queryVariables) {
				if (variableInstance.getId() != null && variableInstance.getTaskId() != null) {
					variables.put(variableInstance.getName(), variableInstance.getValue());
				}
			}
		}
		return variables;
	}

	public Map<String, Object> getProcessVariables() {
		Map<String, Object> variables = new HashMap<String, Object>();
		if (queryVariables != null) {
			for (HistoricVariableInstanceEntity variableInstance : queryVariables) {
				if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
					variables.put(variableInstance.getName(), variableInstance.getValue());
				}
			}
		}
		return variables;
	}

	public List<HistoricVariableInstanceEntity> getQueryVariables() {
		if (queryVariables == null && Context.getCommandContext() != null) {
			queryVariables = new VariableInitializingList();
		}
		return queryVariables;
	}

	public void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables) {
		this.queryVariables = queryVariables;
	}

	/**
	 * List that initialized binary variable values if command-context is
	 * active.
	 * 
	 * @author Frederik Heremans
	 */
	private class VariableInitializingList extends ArrayList<HistoricVariableInstanceEntity> {

		private static final long serialVersionUID = 1L;

		@Override
		public void add(int index, HistoricVariableInstanceEntity e) {
			super.add(index, e);
			initializeBinaryVariable(e);
		}

		@Override
		public boolean add(HistoricVariableInstanceEntity e) {
			initializeBinaryVariable(e);
			return super.add(e);
		}

		@Override
		public boolean addAll(Collection<? extends HistoricVariableInstanceEntity> c) {
			for (HistoricVariableInstanceEntity e : c) {
				initializeBinaryVariable(e);
			}
			return super.addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends HistoricVariableInstanceEntity> c) {
			for (HistoricVariableInstanceEntity e : c) {
				initializeBinaryVariable(e);
			}
			return super.addAll(index, c);
		}

		/**
		 * If the passed {@link VariableInstanceEntity} is a binary variable and
		 * the command-context is active, the variable value is fetched to
		 * ensure the byte-array is populated.
		 */
		@SuppressWarnings("deprecation")
		protected void initializeBinaryVariable(HistoricVariableInstanceEntity e) {
			if (Context.getCommandContext() != null && e != null && e.getByteArrayValueId() != null) {
				e.getValue();
			}
		}
	}
}
