class UniqueCertificationsManager {
    constructor() {
        this.form = document.getElementById('certificationSendForm');
        if (!this.form) {
            return;
        }

        this.selectAllCheckbox = document.getElementById('certificationSelectAll');
        this.checkboxes = Array.from(this.form.querySelectorAll('.certification-checkbox'));
        this.selectionCounter = this.form.querySelector('.certification-selection-counter');
        this.submitButtons = Array.from(this.form.querySelectorAll('button[type="submit"][data-selection-required]'));

        this.registerEvents();
        this.updateSelectionUI();
    }

    registerEvents() {
        if (this.selectAllCheckbox) {
            this.selectAllCheckbox.addEventListener('change', (event) => {
                const checked = event.target.checked;
                this.checkboxes.forEach(checkbox => {
                    if (!checkbox.disabled) {
                        checkbox.checked = checked;
                    }
                });
                this.updateSelectionUI();
            });
        }

        this.checkboxes.forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.syncSelectAllState();
                this.updateSelectionUI();
            });
        });

        this.form.addEventListener('submit', (event) => {
            const submitter = event.submitter;
            const action = submitter?.formAction || this.form.action;

            if (submitter?.hasAttribute('data-selection-required') && this.getSelectedCount() === 0) {
                event.preventDefault();
                return false;
            }

            if (action.includes('/delete') && !this.confirmDeletion(submitter)) {
                event.preventDefault();
                return false;
            }

            if (submitter) {
                this.showLoading(submitter);
            }
            return true;
        });
    }

    syncSelectAllState() {
        if (!this.selectAllCheckbox) {
            return;
        }
        const selectable = this.checkboxes.filter(checkbox => !checkbox.disabled);
        const selected = selectable.filter(checkbox => checkbox.checked);
        this.selectAllCheckbox.checked = selectable.length > 0 && selected.length === selectable.length;
        this.selectAllCheckbox.indeterminate = selected.length > 0 && selected.length < selectable.length;
    }

    updateSelectionUI() {
        const selectedCount = this.getSelectedCount();
        this.submitButtons.forEach(button => {
            button.disabled = selectedCount === 0;
        });
        if (this.selectionCounter) {
            if (selectedCount === 0) {
                this.selectionCounter.style.display = 'none';
            } else {
                this.selectionCounter.style.display = '';
                this.selectionCounter.textContent = `${selectedCount} selezionate`;
            }
        }
    }

    getSelectedCount() {
        return this.checkboxes.filter(checkbox => checkbox.checked && !checkbox.disabled).length;
    }

    confirmDeletion(button) {
        const message = button?.getAttribute('data-confirm-message');
        if (!message) {
            return true;
        }
        return window.confirm(message);
    }

    showLoading(button) {
        button.classList.add('loading');
        button.setAttribute('aria-busy', 'true');
        setTimeout(() => {
            button.classList.remove('loading');
            button.removeAttribute('aria-busy');
        }, 1500);
    }
}

window.addEventListener('DOMContentLoaded', () => new UniqueCertificationsManager());