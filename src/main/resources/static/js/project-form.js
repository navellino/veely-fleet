/**
 * PROJECT FORM MANAGEMENT
 * 
 * This module handles all JavaScript functionality for the project form page.
 * Uses modern ES6+ features and follows modular patterns for maintainability.
 * Integrates with Bootstrap 5.3.2 components and events.
 * 
 * @version 2.1.0
 * @author Frontend Team
 */

/**
 * Main ProjectForm class that orchestrates all form functionality
 * Uses composition pattern to separate concerns into smaller, focused modules
 */
class ProjectForm {
    constructor() {
        this.form = document.getElementById('projectForm');
        this.isInitialized = false;
        
        // Sub-modules for different form sections
        this.contactsManager = null;
        this.invoicesManager = null;
        this.documentsManager = null;
        this.validationManager = null;
        this.progressTracker = null;
        
        // Initialize when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }
    
    /**
     * Initialize all form components and event listeners
     * Sets up the main form functionality and delegates to specialized managers
     */
    init() {
        if (this.isInitialized || !this.form) return;
        
        try {
            // Initialize sub-managers
            this.contactsManager = new ContactsManager(this);
            this.invoicesManager = new InvoicesManager(this);
            this.documentsManager = new DocumentsManager(this);
            this.validationManager = new ValidationManager(this);
            this.progressTracker = new ProgressTracker(this);
            
            // Setup main form events
            this.setupFormEvents();
            this.setupTabNavigation();
            this.setupLoadingStates();
            
            // Apply initial animations
            this.applyInitialAnimations();
            
            this.isInitialized = true;
            console.log('ProjectForm initialized successfully');
            
        } catch (error) {
            console.error('Error initializing ProjectForm:', error);
        }
    }
    
    /**
     * Setup main form submission and global events
     */
    setupFormEvents() {
        // Form submission with validation
        this.form.addEventListener('submit', (e) => {
            if (!this.validationManager.validateAll()) {
                e.preventDefault();
                this.showValidationErrors();
                return false;
            }
            
            this.showLoadingState();
        });
        
        // Auto-save functionality (optional)
        this.setupAutoSave();
    }
    
    /**
     * Setup tab navigation with validation
     * Prevents navigation to next tab if current tab has validation errors
     */
    setupTabNavigation() {
        const tabButtons = document.querySelectorAll('#projectTabs button[data-bs-toggle="tab"]');
		
		const STORAGE_KEY = 'projectForm_activeTab';

		      // Restore previously active tab from storage
		      const storedTab = localStorage.getItem(STORAGE_KEY);
		      if (storedTab) {
		          const targetButton = document.querySelector(`#projectTabs button[data-bs-target="${storedTab}"]`);
		          if (targetButton) {
		              bootstrap.Tab.getOrCreateInstance(targetButton).show();
		          }
		      }
        
        tabButtons.forEach(button => {
            button.addEventListener('show.bs.tab', () => {
                // Optional: validate current tab before switching
                const currentTab = document.querySelector('.tab-pane.active');
                if (currentTab && !this.validationManager.validateTab(currentTab)) {
                    // Could prevent tab switch here if needed
                    console.warn('Current tab has validation errors');
                }
            });
			
			// Save active tab when shown
			           button.addEventListener('shown.bs.tab', (e) => {
			               const target = e.target.getAttribute('data-bs-target');
			               if (target) {
			                   localStorage.setItem(STORAGE_KEY, target);
			               }
			           });
        });
    }
    
    /**
     * Setup loading states for form submission
     */
    setupLoadingStates() {
        const submitButtons = this.form.querySelectorAll('button[type="submit"]');
        
        submitButtons.forEach(button => {
            const originalText = button.innerHTML;
            
            this.form.addEventListener('submit', () => {
                button.disabled = true;
                button.innerHTML = `
                    <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Salvataggio...
                `;
                
                // Re-enable after timeout (fallback for validation errors)
                setTimeout(() => {
                    button.disabled = false;
                    button.innerHTML = originalText;
                }, 5000);
            });
        });
    }
    
    /**
     * Apply initial fade-in animations to form sections
     */
    applyInitialAnimations() {
        const sections = document.querySelectorAll('.form-section.card');
        
        sections.forEach((section, index) => {
            section.style.animationDelay = `${index * 0.1}s`;
            section.classList.add('fade-in-up');
        });
    }
    
    /**
     * Setup auto-save functionality (saves form data to localStorage)
     * Only saves non-sensitive data for user convenience
     */
    setupAutoSave() {
        const autoSaveFields = this.form.querySelectorAll('[data-autosave]');
        
        autoSaveFields.forEach(field => {
            field.addEventListener('input', debounce(() => {
                this.saveFieldToStorage(field);
            }, 1000));
        });
        
        // Restore saved data on page load
        this.restoreAutoSavedData();
    }
    
    /**
     * Save field data to localStorage for auto-save functionality
     */
    saveFieldToStorage(field) {
        try {
            const key = `projectForm_${field.name}`;
            localStorage.setItem(key, field.value);
        } catch (error) {
            console.warn('Could not save field to localStorage:', error);
        }
    }
    
    /**
     * Restore auto-saved data from localStorage
     */
    restoreAutoSavedData() {
        const autoSaveFields = this.form.querySelectorAll('[data-autosave]');
        
        autoSaveFields.forEach(field => {
            try {
                const key = `projectForm_${field.name}`;
                const saved = localStorage.getItem(key);
                if (saved && !field.value) {
                    field.value = saved;
                }
            } catch (error) {
                console.warn('Could not restore field from localStorage:', error);
            }
        });
    }
    
    /**
     * Show loading state for form submission
     */
    showLoadingState() {
        const overlay = document.createElement('div');
        overlay.className = 'position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center';
        overlay.style.backgroundColor = 'rgba(0,0,0,0.5)';
        overlay.style.zIndex = '9999';
        overlay.innerHTML = `
            <div class="bg-white p-4 rounded shadow">
                <div class="d-flex align-items-center">
                    <div class="spinner-border text-primary me-3" role="status"></div>
                    <span>Salvataggio in corso...</span>
                </div>
            </div>
        `;
        
        document.body.appendChild(overlay);
    }
    
    /**
     * Show validation errors with user-friendly messages
     */
    showValidationErrors() {
        const firstError = this.form.querySelector('.is-invalid');
        if (firstError) {
            firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstError.focus();
            
            // Show toast notification
            this.showToast('Correggere gli errori evidenziati prima di procedere', 'warning');
        }
    }
    
    /**
     * Show toast notifications
     */
    showToast(message, type = 'info') {
        // Create toast container if it doesn't exist
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(container);
        }
        
        // Create toast element
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-bg-${type} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        container.appendChild(toast);
        
        // Initialize and show toast
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        // Remove toast element after it's hidden
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
}

/**
 * CONTACTS MANAGER
 * Handles the dynamic contacts table functionality
 */
class ContactsManager {
    constructor(parentForm) {
        this.parentForm = parentForm;
        this.table = document.getElementById('contactsTable');
        this.tbody = this.table?.querySelector('tbody');
        this.addButton = document.getElementById('addContactBtn');
        
        if (this.table && this.addButton) {
            this.init();
        }
    }
    
    init() {
        this.setupEventListeners();
        this.updateRowIndices();
    }
    
    setupEventListeners() {
        // Add contact button
        this.addButton.addEventListener('click', () => {
            this.addContact();
            this.createRippleEffect(this.addButton);
        });
        
        // Delegate remove button events
        this.tbody.addEventListener('click', (e) => {
            if (e.target.closest('.btn-remove-contact')) {
                const row = e.target.closest('tr');
                this.removeContact(row);
            }
        });
    }
    
    /**
     * Add a new contact row to the table
     */
    addContact() {
        // Remove empty state if present
        const emptyRow = this.tbody.querySelector('.empty-state-row');
        if (emptyRow) {
            emptyRow.remove();
        }
        
        const index = this.tbody.children.length;
        const row = this.createContactRow(index);
        
        this.tbody.appendChild(row);
        
        // Focus on first input for better UX
        const firstInput = row.querySelector('input');
        if (firstInput) {
            firstInput.focus();
        }
        
        this.updateRowIndices();
    }
    
    /**
     * Create a new contact row element
     */
    createContactRow(index) {
        const row = document.createElement('tr');
        row.className = 'fade-in-up';
        
        row.innerHTML = `
            <td><input class="form-control form-control-sm" name="contacts[${index}].firstName" placeholder="Nome" /></td>
            <td><input class="form-control form-control-sm" name="contacts[${index}].lastName" placeholder="Cognome" /></td>
            <td><input class="form-control form-control-sm" name="contacts[${index}].role" placeholder="Ruolo" /></td>
            <td><input class="form-control form-control-sm" name="contacts[${index}].phone" placeholder="+39 xxx xxx xxxx" /></td>
            <td><input class="form-control form-control-sm" name="contacts[${index}].email" type="email" placeholder="email@example.com" /></td>
            <td><input class="form-control form-control-sm" name="contacts[${index}].pec" type="email" placeholder="pec@example.com" /></td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-danger btn-sm btn-remove-contact" 
                        title="Rimuovi contatto" aria-label="Rimuovi contatto">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        
        return row;
    }
    
    /**
     * Remove a contact row with animation
     */
    removeContact(row) {
        row.classList.add('slide-out-left');
        
        setTimeout(() => {
            row.remove();
            this.updateRowIndices();
            this.checkEmptyState();
        }, 300);
    }
    
    /**
     * Update row indices after add/remove operations
     */
    updateRowIndices() {
        const rows = this.tbody.querySelectorAll('tr:not(.empty-state-row)');
        
        rows.forEach((row, index) => {
            const inputs = row.querySelectorAll('input');
            inputs.forEach(input => {
                const name = input.getAttribute('name');
                if (name && name.includes('contacts[')) {
                    const newName = name.replace(/contacts\[\d+\]/, `contacts[${index}]`);
                    input.setAttribute('name', newName);
                }
            });
        });
    }
    
    /**
     * Show empty state if no contacts exist
     */
    checkEmptyState() {
        const dataRows = this.tbody.querySelectorAll('tr:not(.empty-state-row)');
        
        if (dataRows.length === 0) {
            const emptyRow = document.createElement('tr');
            emptyRow.className = 'empty-state-row fade-in-up';
            emptyRow.innerHTML = `
                <td colspan="7" class="text-center text-muted py-4">
                    <i class="bi bi-person-plus-fill me-2"></i>
                    Nessun contatto presente. Clicca "Aggiungi Contatto" per iniziare.
                </td>
            `;
            this.tbody.appendChild(emptyRow);
        }
    }
    
    /**
     * Create ripple effect on button click
     */
    createRippleEffect(element) {
        const ripple = document.createElement('span');
        const rect = element.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = rect.width / 2;
        const y = rect.height / 2;
        
        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = (x - size / 2) + 'px';
        ripple.style.top = (y - size / 2) + 'px';
        ripple.classList.add('ripple');
        
        element.style.position = 'relative';
        element.style.overflow = 'hidden';
        element.appendChild(ripple);
        
        setTimeout(() => ripple.remove(), 600);
    }
}

/**
 * INVOICES MANAGER
 * Handles the invoices table and progress tracking
 */
class InvoicesManager {
    constructor(parentForm) {
        this.parentForm = parentForm;
        this.table = document.getElementById('invoicesTable');
        this.tbody = this.table?.querySelector('tbody');
        this.addButton = document.getElementById('addInvoiceBtn');
        
        if (this.table && this.addButton) {
            this.init();
        }
    }
    
    init() {
        this.setupEventListeners();
        this.updateRowIndices();
    }
    
    setupEventListeners() {
        // Add invoice button
        this.addButton.addEventListener('click', () => {
            this.addInvoice();
        });
        
        // Delegate remove button events
        this.tbody.addEventListener('click', (e) => {
            if (e.target.closest('.btn-remove-invoice')) {
                const row = e.target.closest('tr');
                this.removeInvoice(row);
            }
        });
        
        // Listen for amount changes to update progress
        this.tbody.addEventListener('input', (e) => {
            if (e.target.classList.contains('invoice-amount')) {
                this.parentForm.progressTracker?.updateProgress();
            }
        });
        
        // Listen for project value changes
        const projectValueInput = document.getElementById('projectValue');
        if (projectValueInput) {
            projectValueInput.addEventListener('input', () => {
                this.parentForm.progressTracker?.updateProgress();
            });
        }
    }
    
    /**
     * Add a new invoice row
     */
    addInvoice() {
        // Remove empty state if present
        const emptyRow = this.tbody.querySelector('.empty-state-row');
        if (emptyRow) {
            emptyRow.remove();
        }
        
        const index = this.tbody.children.length;
        const row = this.createInvoiceRow(index);
        
        this.tbody.appendChild(row);
        
        // Focus on first input
        const firstInput = row.querySelector('input');
        if (firstInput) {
            firstInput.focus();
        }
        
        this.updateRowIndices();
        this.parentForm.progressTracker?.updateProgress();
    }
    
    /**
     * Create a new invoice row element
     */
    createInvoiceRow(index) {
        const row = document.createElement('tr');
        row.className = 'fade-in-up';
        
        row.innerHTML = `
            <td><input class="form-control form-control-sm" name="invoices[${index}].number" placeholder="Numero" /></td>
            <td><input class="form-control form-control-sm" type="date" name="invoices[${index}].date" /></td>
            <td><input class="form-control form-control-sm" name="invoices[${index}].description" placeholder="Descrizione" /></td>
            <td><input class="form-control form-control-sm invoice-amount" type="number" step="0.01" name="invoices[${index}].amount" placeholder="0.00" /></td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-danger btn-sm btn-remove-invoice" 
                        title="Rimuovi fattura" aria-label="Rimuovi fattura">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        
        return row;
    }
    
    /**
     * Remove an invoice row with animation
     */
    removeInvoice(row) {
        row.classList.add('slide-out-left');
        
        setTimeout(() => {
            row.remove();
            this.updateRowIndices();
            this.checkEmptyState();
            this.parentForm.progressTracker?.updateProgress();
        }, 300);
    }
    
    /**
     * Update row indices after add/remove operations
     */
    updateRowIndices() {
        const rows = this.tbody.querySelectorAll('tr:not(.empty-state-row)');
        
        rows.forEach((row, index) => {
            const inputs = row.querySelectorAll('input');
            inputs.forEach(input => {
                const name = input.getAttribute('name');
                if (name && name.includes('invoices[')) {
                    const newName = name.replace(/invoices\[\d+\]/, `invoices[${index}]`);
                    input.setAttribute('name', newName);
                }
            });
        });
    }
    
    /**
     * Show empty state if no invoices exist
     */
    checkEmptyState() {
        const dataRows = this.tbody.querySelectorAll('tr:not(.empty-state-row)');
        
        if (dataRows.length === 0) {
            const emptyRow = document.createElement('tr');
            emptyRow.className = 'empty-state-row fade-in-up';
            emptyRow.innerHTML = `
                <td colspan="5" class="text-center text-muted py-4">
                    <i class="bi bi-receipt me-2"></i>
                    Nessuna fattura presente. Clicca "Aggiungi Fattura" per iniziare.
                </td>
            `;
            this.tbody.appendChild(emptyRow);
        }
    }
}

/**
 * UTILITY FUNCTIONS
 */

/**
 * Debounce function to limit the rate of function execution
 * Useful for input events and API calls
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Initialize the ProjectForm when the script loads
 * Creates a global instance for potential external access
 */
window.projectForm = new ProjectForm();