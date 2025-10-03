/**
 * Document Management (Payslips & Certifications) Page JavaScript
 *
 * This script handles all client-side interactions for the document management page.
 * It manages the tabbed interface, modal forms, bulk selection, and form submissions
 * for both payslips and unique certifications. It is designed to be reusable and robust.
 */
class DocumentManager {
    /**
     * @param {string} containerId - The ID of the container for a specific document type (e.g., 'payslips-tab-pane').
     * @param {object} selectors - A configuration object with CSS selectors for various elements.
     */
    constructor(containerId, selectors) {
        this.container = document.getElementById(containerId);
        if (!this.container) return; // Exit if the container (e.g., a tab) isn't on the page

        this.selectors = selectors;

        // Bind elements within the specific container
        this.actionForm = this.container.querySelector(selectors.actionForm);
        this.selectAllCheckbox = this.container.querySelector(selectors.selectAllCheckbox);
        this.checkboxes = Array.from(this.container.querySelectorAll(selectors.checkbox));
        this.selectionCounter = this.container.querySelector(selectors.selectionCounter);
        this.sendButton = this.container.querySelector(selectors.sendButton);
        this.deleteButton = this.container.querySelector(selectors.deleteButton);
        
        // Modals are outside the container, so we find them in the global scope
        this.sendModal = document.querySelector(selectors.sendModalSelector);

        this.registerEventListeners();
        this.updateSelectionUI();
    }

    /**
     * Registers all necessary event listeners for the component.
     */
    registerEventListeners() {
        this.selectAllCheckbox?.addEventListener('change', this.handleSelectAll.bind(this));
        this.checkboxes.forEach(cb => cb.addEventListener('change', this.handleSingleCheckboxChange.bind(this)));

        // Attach listener to the main form for deletion confirmation
        this.actionForm?.addEventListener('submit', (event) => {
            if (event.submitter === this.deleteButton && !this.confirmAction(this.deleteButton)) {
                event.preventDefault();
            }
        });

        // Inject selected IDs right before the send form is submitted
        if (this.sendModal) {
            const sendForm = this.sendModal.querySelector('form');
            sendForm?.addEventListener('submit', () => this.injectSelectedIds(sendForm));
        }
    }
    
    /** Handles the 'Select All' checkbox change event. */
    handleSelectAll(event) {
        this.checkboxes.forEach(cb => { if (!cb.disabled) cb.checked = event.target.checked; });
        this.updateSelectionUI();
    }
    
    /** Handles individual checkbox change events. */
    handleSingleCheckboxChange() {
        this.syncSelectAllState();
        this.updateSelectionUI();
    }

    /** Synchronizes the 'Select All' checkbox state based on individual selections. */
    syncSelectAllState() {
        if (!this.selectAllCheckbox) return;
        const selectable = this.checkboxes.filter(cb => !cb.disabled);
        const selected = selectable.filter(cb => cb.checked);
        
        if (selected.length === 0) {
            this.selectAllCheckbox.checked = false;
            this.selectAllCheckbox.indeterminate = false;
        } else if (selected.length < selectable.length) {
            this.selectAllCheckbox.checked = false;
            this.selectAllCheckbox.indeterminate = true;
        } else {
            this.selectAllCheckbox.checked = true;
            this.selectAllCheckbox.indeterminate = false;
        }
    }

    /** Updates UI elements like buttons and counters based on selection. */
    updateSelectionUI() {
        const selectedCount = this.getSelectedIds().length;
        const hasSelection = selectedCount > 0;

        this.sendButton.disabled = !hasSelection;
        this.deleteButton.disabled = !hasSelection;

        if (this.selectionCounter) {
            this.selectionCounter.textContent = hasSelection ? `${selectedCount} selezionat${selectedCount > 1 ? 'i' : 'o'}` : '';
        }
    }
    
    /** Returns an array of IDs of the selected items. */
    getSelectedIds() {
        return this.checkboxes.filter(cb => cb.checked).map(cb => cb.value);
    }
    
    /** Injects selected IDs as hidden inputs into the specified form. */
    injectSelectedIds(form) {
        // Clear previous hidden inputs to prevent duplicates
        form.querySelectorAll(`input[name="${this.selectors.idInputName}"]`).forEach(input => input.remove());
        
        this.getSelectedIds().forEach(id => {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = this.selectors.idInputName;
            input.value = id;
            form.appendChild(input);
        });
    }
    
    /** Shows a confirmation dialog for an action. */
    confirmAction(button) {
        const message = button.getAttribute('data-confirm-message');
        return !message || window.confirm(message);
    }
}

// --- INITIALIZATION ---
document.addEventListener('DOMContentLoaded', () => {
    // Initialize a manager for the Payslips tab
    new DocumentManager('payslips-tab-pane', {
        actionForm: '#payslipActionForm',
        sendModalSelector: '#sendPayslipModal',
        selectAllCheckbox: '#payslipSelectAll',
        checkbox: '.payslip-checkbox',
        selectionCounter: '#payslipSelectionCounter',
        sendButton: '#sendPayslipsBtn',
        deleteButton: '#deletePayslipsBtn',
        idInputName: 'payslipIds'
    });

    // Initialize a manager for the Certifications tab
    new DocumentManager('certifications-tab-pane', {
        actionForm: '#certificationActionForm',
        sendModalSelector: '#sendCertificationModal',
        selectAllCheckbox: '#certificationSelectAll',
        checkbox: '.certification-checkbox',
        selectionCounter: '#certificationSelectionCounter',
        sendButton: '#sendCertificationsBtn',
        deleteButton: '#deleteCertificationsBtn',
        idInputName: 'certificationIds'
    });
    
    // Initialize Animate on Scroll (AOS) library
    if (typeof AOS !== 'undefined') {
        AOS.init({ duration: 600, once: true });
    }
});