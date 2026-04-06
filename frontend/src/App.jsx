import { Fragment, useEffect, useRef, useState } from "react";
import { apiRequest, downloadProtectedFile, openProtectedFile } from "./api";

const emptyTaskForm = {
  title: "",
  description: "",
  priority: "MEDIUM",
  importance: "IMPORTANT",
  urgency: "NOT_URGENT",
  deadline: "",
  assigneeUserId: "",
  attachmentFile: null
};

const userEditableStatuses = ["PENDING", "IN_PROGRESS", "COMPLETED"];
const adminEditableStatuses = ["PENDING", "IN_PROGRESS", "COMPLETED", "APPROVED", "REJECTED"];
const taskPriorities = ["LOW", "MEDIUM", "HIGH"];
const kanbanStatuses = ["PENDING", "IN_PROGRESS", "COMPLETED"];
const urgencyColumns = ["URGENT", "NOT_URGENT"];
const importanceRows = ["IMPORTANT", "NOT_IMPORTANT"];

function DeadlineField({ value, onChange }) {
  const inputRef = useRef(null);

  function openPicker() {
    if (typeof inputRef.current?.showPicker === "function") {
      inputRef.current.showPicker();
      return;
    }
    inputRef.current?.focus();
  }

  return (
    <div className="deadline-field">
      <label htmlFor="task-deadline">Deadline</label>
      <div className="deadline-input-group">
        <input
          id="task-deadline"
          ref={inputRef}
          type="date"
          value={value}
          onChange={onChange}
        />
        <button type="button" className="ghost" onClick={openPicker}>
          Open Calendar
        </button>
      </div>
    </div>
  );
}

function TaskBinaryToggle({ label, value, onChange, options }) {
  return (
    <div className="binary-toggle-field">
      <span className="binary-toggle-label">{label}</span>
      <div
        className="binary-toggle"
        role="group"
        aria-label={label}
        style={{ "--toggle-columns": options.length }}
      >
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            className={value === option.value ? "binary-toggle-option binary-toggle-option-active" : "binary-toggle-option"}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function AuthForm({ mode, onSubmit, switchMode }) {
  const isLogin = mode === "login";
  const [form, setForm] = useState({
    fullName: "",
    username: "",
    email: "",
    password: ""
  });

  function handleChange(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    const payload = isLogin
      ? { username: form.username, password: form.password }
      : form;
    onSubmit(payload);
  }

  return (
    <section className="panel auth-panel">
      <div className="eyebrow">Secure Access</div>
      <h1>Task Management System</h1>
      <p className="muted">JWT authentication, RBAC, approval workflow, and audit visibility.</p>
      <form onSubmit={handleSubmit} className="form-grid">
        {!isLogin && (
          <input name="fullName" placeholder="Full name" value={form.fullName} onChange={handleChange} required />
        )}
        <input name="username" placeholder="Username" value={form.username} onChange={handleChange} required />
        {!isLogin && (
          <input name="email" type="email" placeholder="Email" value={form.email} onChange={handleChange} required />
        )}
        <input
          name="password"
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={handleChange}
          required
        />
        <button type="submit">{isLogin ? "Login" : "Register"}</button>
      </form>
      <button className="ghost" onClick={switchMode}>
        {isLogin ? "Need an account? Register" : "Already registered? Login"}
      </button>
      <div className="credentials">
        <span>Admin: `admin / Admin@123`</span>
        <span>User: `user / User@123`</span>
      </div>
    </section>
  );
}

function FilterBar({ filters, setFilters, refresh, onExport, users, role }) {
  return (
    <div className="toolbar">
      <input
        placeholder="Search title"
        value={filters.search}
        onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value, page: 0 }))}
      />
      <select
        value={filters.status}
        onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value, page: 0 }))}
      >
        <option value="">All active</option>
        <option value="PENDING">Pending</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="COMPLETED">Completed</option>
        <option value="APPROVED">Approved</option>
        <option value="REJECTED">Rejected</option>
      </select>
      <select
        value={filters.priority}
        onChange={(event) => setFilters((current) => ({ ...current, priority: event.target.value, page: 0 }))}
      >
        <option value="">All priorities</option>
        <option value="LOW">Low</option>
        <option value="MEDIUM">Medium</option>
        <option value="HIGH">High</option>
      </select>
      {role === "ADMIN" && (
        <select
          value={filters.ownerUserId}
          onChange={(event) => setFilters((current) => ({ ...current, ownerUserId: event.target.value, page: 0 }))}
        >
          <option value="">All users</option>
          {users.filter((item) => item.role === "USER").map((item) => (
            <option key={item.id} value={item.id}>
              {item.username}
            </option>
          ))}
        </select>
      )}
      <select
        value={filters.exportFormat}
        onChange={(event) => setFilters((current) => ({ ...current, exportFormat: event.target.value }))}
      >
        <option value="pdf">Export as PDF</option>
        <option value="excel">Export as Excel</option>
      </select>
      <select
        value={filters.size}
        onChange={(event) => setFilters((current) => ({ ...current, size: Number(event.target.value), page: 0 }))}
      >
        <option value="5">5 / page</option>
        <option value="10">10 / page</option>
        <option value="20">20 / page</option>
      </select>
      <button onClick={refresh}>Refresh</button>
      <button className="ghost" onClick={onExport}>Export Tasks</button>
    </div>
  );
}

function PaginationBar({ pageInfo, setFilters }) {
  if (!pageInfo) {
    return null;
  }

  return (
    <div className="pagination-bar panel">
      <span className="muted">
        Page {pageInfo.number + 1} of {Math.max(pageInfo.totalPages, 1)} • {pageInfo.totalElements} tasks
      </span>
      <div className="actions">
        <button
          className="ghost"
          disabled={pageInfo.first}
          onClick={() => setFilters((current) => ({ ...current, page: Math.max(current.page - 1, 0) }))}
        >
          Previous
        </button>
        <button
          className="ghost"
          disabled={pageInfo.last}
          onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
        >
          Next
        </button>
      </div>
    </div>
  );
}

function formatStatusLabel(status) {
  return status.replace("_", " ");
}

function formatMatrixLabel(value) {
  return value.replace("_", " ");
}

function toDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

function formatFileSize(bytes) {
  if (!bytes) {
    return "0 B";
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function KanbanBoard({ tasks, onMoveTask }) {
  const [activeColumn, setActiveColumn] = useState("");
  const visibleTasks = tasks.filter((task) => kanbanStatuses.includes(task.status));
  const hiddenCount = tasks.length - visibleTasks.length;

  function handleDrop(event, nextStatus) {
    event.preventDefault();
    setActiveColumn("");
    const taskId = Number(event.dataTransfer.getData("text/task-id"));
    const task = visibleTasks.find((item) => item.id === taskId);
    if (!task || task.status === nextStatus) {
      return;
    }
    onMoveTask(task, nextStatus);
  }

  return (
    <section className="panel">
      <div className="kanban-head">
        <div>
          <div className="eyebrow">Kanban View</div>
          <h2>Drag Tasks Across Workflow</h2>
        </div>
        {hiddenCount > 0 && (
          <span className="muted">{hiddenCount} approved or rejected task(s) are not shown here.</span>
        )}
      </div>
      <div className="kanban-grid">
        {kanbanStatuses.map((status) => (
          <div
            key={status}
            className={`kanban-column${activeColumn === status ? " kanban-column-active" : ""}`}
            onDragOver={(event) => {
              event.preventDefault();
              setActiveColumn(status);
            }}
            onDragLeave={() => setActiveColumn((current) => (current === status ? "" : current))}
            onDrop={(event) => handleDrop(event, status)}
          >
            <div className="kanban-column-head">
              <strong>{formatStatusLabel(status)}</strong>
              <span className={`badge badge-${status.toLowerCase()}`}>
                {visibleTasks.filter((task) => task.status === status).length}
              </span>
            </div>
            <div className="kanban-card-list">
              {visibleTasks.filter((task) => task.status === status).map((task) => (
                <article
                  key={task.id}
                  className="kanban-card"
                  draggable
                  onDragStart={(event) => {
                    event.dataTransfer.setData("text/task-id", String(task.id));
                    event.dataTransfer.effectAllowed = "move";
                  }}
                >
                  <div className="kanban-card-top">
                    <strong>{task.title}</strong>
                    <span className={`badge badge-priority badge-priority-${task.priority.toLowerCase()}`}>
                      {task.priority}
                    </span>
                  </div>
                  <div className="muted">{task.description || "No description"}</div>
                  <div className="kanban-meta">
                    <span>#{task.id}</span>
                    <span>{task.deadline || "No deadline"}</span>
                  </div>
                </article>
              ))}
              {visibleTasks.every((task) => task.status !== status) && (
                <div className="kanban-empty muted">Drop a task here</div>
              )}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function PriorityMatrixBoard({ tasks, onMoveTask }) {
  const [activeCell, setActiveCell] = useState("");
  const visibleTasks = tasks.filter((task) => kanbanStatuses.includes(task.status));

  function getCellTasks(row, column) {
    return visibleTasks.filter((task) => task.importance === row && task.urgency === column);
  }

  function handleDrop(event, row, column) {
    event.preventDefault();
    setActiveCell("");
    const taskId = Number(event.dataTransfer.getData("text/task-id"));
    const task = visibleTasks.find((item) => item.id === taskId);
    if (!task) {
      return;
    }
    onMoveTask(task, row, column);
  }

  return (
    <section className="panel">
      <div className="kanban-head">
        <div>
          <div className="eyebrow">Priority Matrix</div>
          <h2>Important vs Urgent</h2>
        </div>
        <span className="muted">Columns are urgency. Rows are importance.</span>
      </div>
      <div className="matrix-board">
        <div className="matrix-axis-corner" />
        {urgencyColumns.map((column) => (
          <div key={column} className="matrix-axis">
            {formatMatrixLabel(column)}
          </div>
        ))}
        {importanceRows.map((row) => (
          <Fragment key={row}>
            <div key={`${row}-label`} className="matrix-axis matrix-axis-vertical">
              {formatMatrixLabel(row)}
            </div>
            {urgencyColumns.map((column) => {
              const cellKey = `${row}-${column}`;
              return (
                <div
                  key={cellKey}
                  className={`matrix-cell${activeCell === cellKey ? " matrix-cell-active" : ""}`}
                  onDragOver={(event) => {
                    event.preventDefault();
                    setActiveCell(cellKey);
                  }}
                  onDragLeave={() => setActiveCell((current) => (current === cellKey ? "" : current))}
                  onDrop={(event) => handleDrop(event, row, column)}
                >
                  <div className="matrix-cell-head">
                    <strong>{formatMatrixLabel(row)} / {formatMatrixLabel(column)}</strong>
                    <span className="muted">{getCellTasks(row, column).length} task(s)</span>
                  </div>
                  <div className="matrix-card-list">
                    {getCellTasks(row, column).map((task) => (
                      <article
                        key={task.id}
                        className="kanban-card"
                        draggable
                        onDragStart={(event) => {
                          event.dataTransfer.setData("text/task-id", String(task.id));
                          event.dataTransfer.effectAllowed = "move";
                        }}
                      >
                        <div className="kanban-card-top">
                          <strong>{task.title}</strong>
                          <span className={`badge badge-priority badge-priority-${task.priority.toLowerCase()}`}>
                            {task.priority}
                          </span>
                        </div>
                        <div className="muted">{task.description || "No description"}</div>
                        <div className="kanban-meta">
                          <span>#{task.id}</span>
                          <span>{task.deadline || "No deadline"}</span>
                        </div>
                      </article>
                    ))}
                    {getCellTasks(row, column).length === 0 && (
                      <div className="kanban-empty muted">Drop a task here</div>
                    )}
                  </div>
                </div>
              );
            })}
          </Fragment>
        ))}
      </div>
    </section>
  );
}

function UserSidebar({ activeView, setActiveView }) {
  const items = [
    { id: "kanban", label: "Kanban Board", hint: "Drag tasks across workflow" },
    { id: "priority-matrix", label: "Priority Matrix", hint: "Manage important vs urgent work" },
    { id: "backlog", label: "Backlog Tasks", hint: "Browse and manage task details" }
  ];

  return (
    <aside className="panel sidebar-panel">
      <div className="eyebrow">Workspace</div>
      <h2>Views</h2>
      <div className="sidebar-nav">
        {items.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`sidebar-link${activeView === item.id ? " sidebar-link-active" : ""}`}
            onClick={() => setActiveView(item.id)}
          >
            <strong>{item.label}</strong>
            <span>{item.hint}</span>
          </button>
        ))}
      </div>
    </aside>
  );
}

function TaskComposer({ form, setForm, onCreate, users, role, fileInputKey }) {
  return (
    <section className="panel">
      <h2>{role === "ADMIN" ? "Assign Task" : "Create Task"}</h2>
      <div className="form-grid">
        <input
          placeholder="Task title"
          value={form.title}
          onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
        />
        <textarea
          placeholder="Description"
          rows="4"
          value={form.description}
          onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
        />
        <TaskBinaryToggle
          label="Priority"
          value={form.priority}
          onChange={(nextValue) => setForm((current) => ({ ...current, priority: nextValue }))}
          options={taskPriorities.map((priority) => ({ value: priority, label: priority }))}
        />
        <TaskBinaryToggle
          label="Importance"
          value={form.importance}
          onChange={(nextValue) => setForm((current) => ({ ...current, importance: nextValue }))}
          options={[
            { value: "IMPORTANT", label: "Important" },
            { value: "NOT_IMPORTANT", label: "Not Important" }
          ]}
        />
        <TaskBinaryToggle
          label="Urgency"
          value={form.urgency}
          onChange={(nextValue) => setForm((current) => ({ ...current, urgency: nextValue }))}
          options={[
            { value: "URGENT", label: "Urgent" },
            { value: "NOT_URGENT", label: "Not Urgent" }
          ]}
        />
        <DeadlineField
          value={form.deadline}
          onChange={(event) => setForm((current) => ({ ...current, deadline: event.target.value }))}
        />
        <input
          key={fileInputKey}
          type="file"
          accept=".png,.pdf,.doc,.docx,.xls,.xlsx"
          onChange={(event) => setForm((current) => ({ ...current, attachmentFile: event.target.files?.[0] || null }))}
        />
        {role === "ADMIN" && (
          <select
            value={form.assigneeUserId}
            onChange={(event) => setForm((current) => ({ ...current, assigneeUserId: event.target.value }))}
          >
            <option value="">Assign to user</option>
            {users.filter((item) => item.role === "USER" && item.status === "ACTIVE").map((item) => (
              <option key={item.id} value={item.id}>{item.username}</option>
            ))}
          </select>
        )}
        <button onClick={onCreate}>{role === "ADMIN" ? "Assign Task" : "Create Task"}</button>
      </div>
    </section>
  );
}

function TaskTable({ tasks, role, users, onSubmitTask, onDeleteTask, onReviewTask, onAssignTask, onEditTask, onOpenAttachment }) {
  return (
    <section className="panel">
      <h2>{role === "ADMIN" ? "All Tasks" : "My Tasks"}</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Deadline</th>
              <th>Owner</th>
              <th>Review</th>
              {role === "ADMIN" && <th>Assign</th>}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((task) => (
              <tr key={task.id}>
                <td>{task.id}</td>
                <td>
                  <strong>{task.title}</strong>
                  <div className="muted">{task.description}</div>
                  {task.attachments?.length > 0 && (
                    <div className="attachment-list">
                      {task.attachments.map((attachment) => (
                        <div key={attachment.id} className="attachment-item">
                          <div>
                            <strong>{attachment.originalFilename}</strong>
                            <div className="muted">{attachment.contentType} • {formatFileSize(attachment.fileSize)}</div>
                          </div>
                          <button className="ghost" onClick={() => onOpenAttachment(task.id, attachment.id)}>
                            View File
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                  {task.comments?.length > 0 && (
                    <div className="comment-list">
                      {task.comments.map((comment) => (
                        <div key={comment.id} className="comment-item">
                          <strong>{comment.authorUsername}</strong>: {comment.content}
                          <div className="muted">{new Date(comment.createdAt).toLocaleString()}</div>
                        </div>
                      ))}
                    </div>
                  )}
                </td>
                <td>
                  <span className={`badge badge-${task.status.toLowerCase()}`}>{task.status}</span>
                </td>
                <td>
                  <span className={`badge badge-priority badge-priority-${task.priority.toLowerCase()}`}>
                    {task.priority}
                  </span>
                </td>
                <td>{task.deadline || "-"}</td>
                <td>{task.ownerUsername}</td>
                <td>{task.reviewComment || "-"}</td>
                {role === "ADMIN" && (
                  <td>
                    <select
                      defaultValue=""
                      onChange={(event) => {
                        const value = event.target.value;
                        if (value) {
                          onAssignTask(task.id, value);
                          event.target.value = "";
                        }
                      }}
                    >
                      <option value="">Reassign</option>
                      {users.filter((item) => item.role === "USER" && item.status === "ACTIVE").map((item) => (
                        <option key={item.id} value={item.id}>{item.username}</option>
                      ))}
                    </select>
                  </td>
                )}
                <td className="actions">
                  {(role === "ADMIN" || task.status !== "APPROVED") && (
                    <button onClick={() => onEditTask(task)}>Edit</button>
                  )}
                  {role === "USER" && task.status === "COMPLETED" && (
                    <button onClick={() => onSubmitTask(task.id)}>Submit</button>
                  )}
                  {role === "USER" && task.status !== "APPROVED" && (
                    <button className="ghost" onClick={() => onDeleteTask(task.id)}>Delete</button>
                  )}
                  {role === "ADMIN" && (
                    <button className="ghost" onClick={() => onDeleteTask(task.id)}>Delete</button>
                  )}
                  {role === "ADMIN" && task.status === "COMPLETED" && (
                    <>
                      <button onClick={() => onReviewTask(task.id, "approve")}>Approve</button>
                      <button className="ghost" onClick={() => onReviewTask(task.id, "reject")}>Reject</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function TaskEditModal({ role, task, form, setForm, onClose, onSave, fileInputKey }) {
  if (!task) {
    return null;
  }

  const allowedStatuses = role === "ADMIN" ? adminEditableStatuses : userEditableStatuses;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <section className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-head">
          <div>
            <div className="eyebrow">Task Editor</div>
            <h2>Edit Task #{task.id}</h2>
          </div>
          <button className="ghost" onClick={onClose}>Close</button>
        </div>
        <div className="form-grid">
          <input
            placeholder="Task title"
            value={form.title}
            onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
          />
          <textarea
            rows="5"
            placeholder="Description"
            value={form.description}
            onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
          />
          <TaskBinaryToggle
            label="Priority"
            value={form.priority}
            onChange={(nextValue) => setForm((current) => ({ ...current, priority: nextValue }))}
            options={taskPriorities.map((priority) => ({ value: priority, label: priority }))}
          />
          <TaskBinaryToggle
            label="Importance"
            value={form.importance}
            onChange={(nextValue) => setForm((current) => ({ ...current, importance: nextValue }))}
            options={[
              { value: "IMPORTANT", label: "Important" },
              { value: "NOT_IMPORTANT", label: "Not Important" }
            ]}
          />
          <TaskBinaryToggle
            label="Urgency"
            value={form.urgency}
            onChange={(nextValue) => setForm((current) => ({ ...current, urgency: nextValue }))}
            options={[
              { value: "URGENT", label: "Urgent" },
              { value: "NOT_URGENT", label: "Not Urgent" }
            ]}
          />
          <DeadlineField
            value={form.deadline}
            onChange={(event) => setForm((current) => ({ ...current, deadline: event.target.value }))}
          />
          <input
            key={fileInputKey}
            type="file"
            accept=".png,.pdf,.doc,.docx,.xls,.xlsx"
            onChange={(event) => setForm((current) => ({ ...current, attachmentFile: event.target.files?.[0] || null }))}
          />
          <select
            value={form.status}
            onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
          >
            {allowedStatuses.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
          <button onClick={onSave}>Save Changes</button>
        </div>
      </section>
    </div>
  );
}

function AdminPanel({ users, audits, onToggleStatus }) {
  return (
    <div className="admin-grid">
      <section className="panel">
        <h2>Users</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Role</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td>{user.role}</td>
                  <td>{user.status}</td>
                  <td>
                    {user.role !== "ADMIN" && (
                      <button onClick={() => onToggleStatus(user)}>
                        {user.status === "ACTIVE" ? "Deactivate" : "Activate"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section className="panel">
        <h2>Audit Trail</h2>
        <ul className="audit-list">
          {audits.map((log) => (
            <li key={log.id}>
              <strong>{log.actor}</strong> {log.action} {log.targetType} #{log.targetId}
              <div className="muted">{new Date(log.createdAt).toLocaleString()}</div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export default function App() {
  const [authMode, setAuthMode] = useState("login");
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem("user");
    return raw ? JSON.parse(raw) : null;
  });
  const [tasks, setTasks] = useState([]);
  const [taskPage, setTaskPage] = useState(null);
  const [users, setUsers] = useState([]);
  const [audits, setAudits] = useState([]);
  const [filters, setFilters] = useState({
    search: "",
    status: "",
    priority: "",
    ownerUserId: "",
    exportFormat: "pdf",
    page: 0,
    size: 10
  });
  const [taskForm, setTaskForm] = useState(emptyTaskForm);
  const [commentDrafts, setCommentDrafts] = useState({});
  const [editingTask, setEditingTask] = useState(null);
  const [editForm, setEditForm] = useState({
    title: "",
    description: "",
    priority: "MEDIUM",
    importance: "IMPORTANT",
    urgency: "NOT_URGENT",
    deadline: "",
    status: "PENDING",
    attachmentFile: null
  });
  const [taskFileInputKey, setTaskFileInputKey] = useState(0);
  const [editFileInputKey, setEditFileInputKey] = useState(0);
  const [userView, setUserView] = useState("kanban");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function saveTaskStatus(task, status) {
    await apiRequest(`/tasks/${task.id}`, {
      method: "PUT",
      token,
      body: {
        title: task.title,
        description: task.description,
        priority: task.priority,
        importance: task.importance,
        urgency: task.urgency,
        deadline: task.deadline || null,
        status
      }
    });
  }

  async function saveTaskMatrixPlacement(task, row, column) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const nextDay = new Date(today);
    nextDay.setDate(today.getDate() + 1);
    const nextWeek = new Date(today);
    nextWeek.setDate(today.getDate() + 7);

    await apiRequest(`/tasks/${task.id}`, {
      method: "PUT",
      token,
      body: {
        title: task.title,
        description: task.description,
        priority: row === "IMPORTANT" ? "HIGH" : "LOW",
        importance: row,
        urgency: column,
        deadline: column === "URGENT" ? toDateInputValue(nextDay) : toDateInputValue(nextWeek),
        status: task.status
      }
    });
  }

  async function run(action, successMessage) {
    try {
      setLoading(true);
      await action();
      setMessage(successMessage);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function authenticate(path, payload) {
    await run(async () => {
      const response = await apiRequest(path, { method: "POST", body: payload });
      setToken(response.token);
      setUser(response.user);
      localStorage.setItem("token", response.token);
      localStorage.setItem("user", JSON.stringify(response.user));
    }, "Authenticated");
  }

  async function loadDashboardData() {
    if (!token || !user) {
      return;
    }

    const params = new URLSearchParams();
    if (filters.search) {
      params.set("search", filters.search);
    }
    if (filters.status) {
      params.set("status", filters.status);
    }
    if (filters.priority) {
      params.set("priority", filters.priority);
    }
    if (user.role === "ADMIN" && filters.ownerUserId) {
      params.set("ownerUserId", filters.ownerUserId);
    }
    params.set("page", String(filters.page));
    params.set("size", String(filters.size));

    await run(async () => {
      const taskResponse = await apiRequest(`/tasks?${params.toString()}`, { token });
      setTasks(taskResponse.content || []);
      setTaskPage({
        number: taskResponse.number,
        totalPages: taskResponse.totalPages,
        totalElements: taskResponse.totalElements,
        first: taskResponse.first,
        last: taskResponse.last
      });
      if (user.role === "ADMIN") {
        const [userResponse, auditResponse] = await Promise.all([
          apiRequest("/users", { token }),
          apiRequest("/audit-logs", { token })
        ]);
        setUsers(userResponse.content || []);
        setAudits(auditResponse.content || []);
      }
    }, "Data refreshed");
  }

  function buildTaskFilterQuery() {
    const params = new URLSearchParams();
    if (filters.search) {
      params.set("search", filters.search);
    }
    if (filters.status) {
      params.set("status", filters.status);
    }
    if (filters.priority) {
      params.set("priority", filters.priority);
    }
    if (user.role === "ADMIN" && filters.ownerUserId) {
      params.set("ownerUserId", filters.ownerUserId);
    }
    return params.toString();
  }

  useEffect(() => {
    loadDashboardData();
  }, [token, user, filters.status, filters.priority, filters.ownerUserId, filters.page, filters.size]);

  function logout() {
    setToken("");
    setUser(null);
    setTasks([]);
    setUsers([]);
    setAudits([]);
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  }

  function openEditTask(task) {
    setEditingTask(task);
    setEditForm({
      title: task.title,
      description: task.description,
      priority: task.priority,
      importance: task.importance || "IMPORTANT",
      urgency: task.urgency || "NOT_URGENT",
      deadline: task.deadline || "",
      status: task.status,
      attachmentFile: null
    });
    setEditFileInputKey((current) => current + 1);
  }

  if (!token || !user) {
    return (
      <main className="shell">
        <AuthForm
          mode={authMode}
          onSubmit={(payload) => authenticate(authMode === "login" ? "/auth/login" : "/auth/register", payload)}
          switchMode={() => setAuthMode((current) => (current === "login" ? "register" : "login"))}
        />
        {message && <div className="toast">{message}</div>}
      </main>
    );
  }

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <div className="eyebrow">Signed in as {user.username}</div>
          <h1>{user.role === "ADMIN" ? "Admin Command View" : "Personal Task Desk"}</h1>
          <p className="muted">Profile: {user.fullName} • {user.email} • {user.status}</p>
        </div>
        <button className="ghost" onClick={logout}>Logout</button>
      </section>

      <FilterBar
        filters={filters}
        setFilters={setFilters}
        refresh={loadDashboardData}
        users={users}
        role={user.role}
        onExport={() => run(async () => {
          const query = buildTaskFilterQuery();
          const path = filters.exportFormat === "excel" ? "/tasks/export/excel" : "/tasks/export/pdf";
          const filename = filters.exportFormat === "excel" ? "tasks-export.xlsx" : "tasks-export.pdf";
          await downloadProtectedFile(`${path}${query ? `?${query}` : ""}`, token, filename);
        }, `${filters.exportFormat.toUpperCase()} downloaded`)}
      />
      <PaginationBar pageInfo={taskPage} setFilters={setFilters} />

      {user.role === "USER" && (
        <section className="workspace-shell">
          <UserSidebar activeView={userView} setActiveView={setUserView} />
          <div className="workspace-main">
            {userView === "kanban" && (
              <KanbanBoard
                tasks={tasks}
                onMoveTask={(task, status) => run(async () => {
                  await saveTaskStatus(task, status);
                  await loadDashboardData();
                }, `Task moved to ${formatStatusLabel(status)}`)}
              />
            )}

            {userView === "priority-matrix" && (
              <PriorityMatrixBoard
                tasks={tasks}
                onMoveTask={(task, row, column) => run(async () => {
                  await saveTaskMatrixPlacement(task, row, column);
                  await loadDashboardData();
                }, `Task moved to ${formatMatrixLabel(row)} / ${formatMatrixLabel(column)}`)}
              />
            )}

            {userView === "backlog" && (
              <TaskTable
                tasks={tasks}
                role={user.role}
                users={users}
                onSubmitTask={(taskId) => run(async () => {
                  await apiRequest(`/tasks/${taskId}/submit`, { method: "POST", token });
                  await loadDashboardData();
                }, "Task submitted")}
                onDeleteTask={(taskId) => run(async () => {
                  await apiRequest(`/tasks/${taskId}`, { method: "DELETE", token });
                  await loadDashboardData();
                }, "Task deleted")}
                onReviewTask={() => {}}
                onAssignTask={() => {}}
                onOpenAttachment={(taskId, attachmentId) => run(async () => {
                  await openProtectedFile(`/tasks/${taskId}/attachments/${attachmentId}`, token);
                }, "Attachment opened")}
                onEditTask={openEditTask}
              />
            )}
          </div>
        </section>
      )}

      {(user.role === "USER" || user.role === "ADMIN") && (
        <TaskComposer
          form={taskForm}
          setForm={setTaskForm}
          users={users}
          role={user.role}
          fileInputKey={taskFileInputKey}
          onCreate={() => run(async () => {
            const body = {
              title: taskForm.title,
              description: taskForm.description,
              priority: taskForm.priority,
              importance: taskForm.importance,
              urgency: taskForm.urgency,
              deadline: taskForm.deadline || null
            };
            if (user.role === "ADMIN" && taskForm.assigneeUserId) {
              body.assigneeUserId = Number(taskForm.assigneeUserId);
            }
            const createdTask = await apiRequest("/tasks", { method: "POST", token, body });
            if (taskForm.attachmentFile) {
              const formData = new FormData();
              formData.append("file", taskForm.attachmentFile);
              await apiRequest(`/tasks/${createdTask.id}/attachments`, { method: "POST", token, body: formData });
            }
            setTaskForm(emptyTaskForm);
            setTaskFileInputKey((current) => current + 1);
            await loadDashboardData();
          }, user.role === "ADMIN" ? "Task assigned" : "Task created")}
        />
      )}

      {user.role === "ADMIN" && (
        <TaskTable
          tasks={tasks}
          role={user.role}
          users={users}
          onSubmitTask={(taskId) => run(async () => {
            await apiRequest(`/tasks/${taskId}/submit`, { method: "POST", token });
            await loadDashboardData();
          }, "Task submitted")}
          onDeleteTask={(taskId) => run(async () => {
            await apiRequest(`/tasks/${taskId}`, { method: "DELETE", token });
            await loadDashboardData();
          }, "Task deleted")}
          onReviewTask={(taskId, action) => run(async () => {
            const comment = window.prompt(`Optional ${action} comment`) || "";
            await apiRequest(`/tasks/${taskId}/${action}`, { method: "POST", token, body: { comment } });
            await loadDashboardData();
          }, `Task ${action}d`)}
          onAssignTask={(taskId, assigneeUserId) => run(async () => {
            await apiRequest(`/tasks/${taskId}/assign`, {
              method: "POST",
              token,
              body: { assigneeUserId: Number(assigneeUserId) }
            });
            await loadDashboardData();
          }, "Task reassigned")}
          onOpenAttachment={(taskId, attachmentId) => run(async () => {
            await openProtectedFile(`/tasks/${taskId}/attachments/${attachmentId}`, token);
          }, "Attachment opened")}
          onEditTask={openEditTask}
        />
      )}

      <TaskEditModal
        role={user.role}
        task={editingTask}
        form={editForm}
        setForm={setEditForm}
        fileInputKey={editFileInputKey}
        onClose={() => setEditingTask(null)}
        onSave={() => run(async () => {
          await apiRequest(`/tasks/${editingTask.id}`, {
            method: "PUT",
            token,
            body: {
              title: editForm.title,
              description: editForm.description,
              priority: editForm.priority,
              importance: editForm.importance,
              urgency: editForm.urgency,
              deadline: editForm.deadline || null,
              status: editForm.status
            }
          });
          if (editForm.attachmentFile) {
            const formData = new FormData();
            formData.append("file", editForm.attachmentFile);
            await apiRequest(`/tasks/${editingTask.id}/attachments`, { method: "POST", token, body: formData });
          }
          setEditingTask(null);
          setEditFileInputKey((current) => current + 1);
          await loadDashboardData();
        }, "Task updated")}
      />

      <section className="panel">
        <h2>Task Comments</h2>
        <div className="comment-grid">
          {tasks.map((task) => (
            <div key={`comment-${task.id}`} className="comment-card">
              <div className="comment-head">
                <strong>#{task.id} {task.title}</strong>
                <span className="muted">{task.ownerUsername}</span>
              </div>
              <div className="comment-feed">
                {(task.comments || []).map((comment) => (
                  <div key={comment.id} className="comment-item">
                    <strong>{comment.authorUsername}</strong>: {comment.content}
                    <div className="muted">{new Date(comment.createdAt).toLocaleString()}</div>
                  </div>
                ))}
              </div>
              <div className="comment-form">
                <textarea
                  rows="2"
                  placeholder="Add a comment"
                  value={commentDrafts[task.id] || ""}
                  onChange={(event) => setCommentDrafts((current) => ({ ...current, [task.id]: event.target.value }))}
                />
                <button onClick={() => run(async () => {
                  await apiRequest(`/tasks/${task.id}/comments`, {
                    method: "POST",
                    token,
                    body: { content: commentDrafts[task.id] || "" }
                  });
                  setCommentDrafts((current) => ({ ...current, [task.id]: "" }));
                  await loadDashboardData();
                }, "Comment added")}>Post Comment</button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {user.role === "ADMIN" && (
        <AdminPanel
          users={users}
          audits={audits}
          onToggleStatus={(targetUser) => run(async () => {
            const nextStatus = targetUser.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
            await apiRequest(`/users/${targetUser.id}/status`, {
              method: "PATCH",
              token,
              body: { status: nextStatus }
            });
            await loadDashboardData();
          }, "User status updated")}
        />
      )}

      {message && <div className="toast">{loading ? "Working..." : message}</div>}
    </main>
  );
}
