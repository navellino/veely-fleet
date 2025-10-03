/**
 * Enhanced Contract Table JavaScript
 * 
 * Features implemented:
 * - Table sorting functionality
 * - Search and filtering
 * - Enhanced user interactions
 * - Accessibility improvements
 * - Performance optimizations
 * 
 * Dependencies: Bootstrap 5.3+, Bootstrap Icons
 */

class ContractTableManager {
    constructor() {
        // Configuration
        this.config = {
            tableSelector: '.contracts-table',
            containerSelector: '.contracts-table-container',
            searchDelay: 300, // Debounce delay for search
            animationDuration: 150
        };
        
        // State management
        this.state = {
            sortColumn: null,
            sortDirection: 'asc',
            searchTerm: '',
            currentData: []
        };
        
        // Cache DOM elements
        this.elements = {};
        
        this.init();
    }
    
    /**
     * Initialize the contract table manager
     */
    init() {
        this.cacheElements();
        this.bindEvents();
        this.enhanceTable();
        this.setupAccessibility();
        
        console.log('ContractTableManager initialized');
    }
    
    /**
     * Cache frequently used DOM elements for performance
     */
    cacheElements() {
        this.elements = {
            table: document.querySelector(this.config.tableSelector),
            container: document.querySelector(this.config.containerSelector),
            tbody: document.querySelector(`${this.config.tableSelector} tbody`),
            headers: document.querySelectorAll(`${this.config.tableSelector} thead th`),
            rows: document.querySelectorAll(`${this.config.tableSelector} tbody tr:not(.empty-row)`)
        };
        
        // Validate required elements exist
        if (!this.elements.table) {
            console.warn('Contract table not found');
            return;
        }
    }
    
    /**
     * Bind event listeners
     */
    bindEvents() {
        if (!this.elements.table) return;
        
        // Header click events for sorting
        this.elements.headers.forEach((header, index) => {
            // Skip action column (usually last)
            if (index === this.elements.headers.length - 1) return;
            
            header.classList.add('sortable');
            header.addEventListener('click', (e) => this.handleSort(e, index));
            
            // Keyboard support for sorting
            header.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    this.handleSort(e, index);
                }
            });
        });
        
        // Delete button confirmation
        this.bindDeleteConfirmation();
        
        // Add search functionality if container exists
        if (this.elements.container) {
            this.addSearchControls();
        }
    }
    
    /**
     * Handle table sorting
     * @param {Event} event - Click event
     * @param {number} columnIndex - Index of the column to sort
     */
    handleSort(event, columnIndex) {
        const header = event.target.closest('th');
        const column = this.getColumnName(columnIndex);
        
        if (!column || !this.elements.rows.length) return;
        
        // Update sort state
        if (this.state.sortColumn === column) {
            this.state.sortDirection = this.state.sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            this.state.sortColumn = column;
            this.state.sortDirection = 'asc';
        }
        
        // Update header classes
        this.updateSortHeaders(header);
        
        // Sort the rows
        this.sortRows(column, this.state.sortDirection);
        
        // Announce sort change to screen readers
        this.announceSort(column, this.state.sortDirection);
    }
    
    /**
     * Get column name from index
     * @param {number} index - Column index
     * @returns {string} Column name
     */
    getColumnName(index) {
        const columnNames = ['subject', 'supplier', 'project', 'status', 'amount'];
        return columnNames[index] || null;
    }
    
    /**
     * Update header sorting classes
     * @param {HTMLElement} activeHeader - Currently clicked header
     */
    updateSortHeaders(activeHeader) {
        // Remove all sort classes
        this.elements.headers.forEach(header => {
            header.classList.remove('sort-asc', 'sort-desc');
        });
        
        // Add appropriate class to active header
        const sortClass = this.state.sortDirection === 'asc' ? 'sort-asc' : 'sort-desc';
        activeHeader.classList.add(sortClass);
    }
    
    /**
     * Sort table rows
     * @param {string} column - Column to sort by
     * @param {string} direction - Sort direction ('asc' or 'desc')
     */
    sortRows(column, direction) {
        const rowsArray = Array.from(this.elements.rows);
        
        rowsArray.sort((a, b) => {
            const aValue = this.getCellValue(a, column);
            const bValue = this.getCellValue(b, column);
            
            let comparison = 0;
            
            // Handle different data types
            if (column === 'amount') {
                comparison = this.compareNumbers(aValue, bValue);
            } else {
                comparison = this.compareStrings(aValue, bValue);
            }
            
            return direction === 'desc' ? -comparison : comparison;
        });
        
        // Re-append sorted rows with animation
        this.reorderRows(rowsArray);
    }
    
    /**
     * Get cell value for sorting
     * @param {HTMLElement} row - Table row
     * @param {string} column - Column name
     * @returns {string} Cell value
     */
    getCellValue(row, column) {
        const columnMap = {
            subject: 0,
            supplier: 1,
            project: 2,
            status: 3,
            amount: 4
        };
        
        const cellIndex = columnMap[column];
        const cell = row.cells[cellIndex];
        
        if (!cell) return '';
        
        // Special handling for different column types
        switch (column) {
            case 'amount':
                // Extract numeric value from formatted amount
                const amountText = cell.querySelector('.contract-amount')?.textContent || '0';
                return amountText.replace(/[^\d,.-]/g, '').replace(',', '.');
            case 'status':
                return cell.querySelector('.status-badge')?.textContent?.trim() || '';
            case 'subject':
                return cell.querySelector('.contract-subject')?.textContent?.trim() || '';
            case 'supplier':
                return cell.querySelector('.supplier-name')?.textContent?.trim() || '';
            case 'project':
                return cell.querySelector('.project-code')?.textContent?.trim() || '';
            default:
                return cell.textContent?.trim() || '';
        }
    }
    
    /**
     * Compare numeric values
     * @param {string} a - First value
     * @param {string} b - Second value
     * @returns {number} Comparison result
     */
    compareNumbers(a, b) {
        const numA = parseFloat(a) || 0;
        const numB = parseFloat(b) || 0;
        return numA - numB;
    }
    
    /**
     * Compare string values
     * @param {string} a - First value
     * @param {string} b - Second value
     * @returns {number} Comparison result
     */
    compareStrings(a, b) {
        return a.localeCompare(b, 'it', { sensitivity: 'base' });
    }
    
    /**
     * Reorder rows with subtle animation
     * @param {HTMLElement[]} sortedRows - Array of sorted rows
     */
    reorderRows(sortedRows) {
        // Add loading state
        this.showLoading(true);
        
        setTimeout(() => {
            // Clear tbody and re-append sorted rows
            const tbody = this.elements.tbody;
            
            // Keep empty row if it exists
            const emptyRow = tbody.querySelector('.empty-row');
            tbody.innerHTML = '';
            
            sortedRows.forEach(row => {
                tbody.appendChild(row);
            });
            
            // Re-append empty row if it existed
            if (emptyRow && sortedRows.length === 0) {
                tbody.appendChild(emptyRow);
            }
            
            this.showLoading(false);
        }, this.config.animationDuration);
    }
    
    /**
     * Add search and filter controls
     */
    addSearchControls() {
        // Create controls container
        const controlsHtml = `
            <div class="table-controls">
                <div class="row align-items-center">
                    <div class="col-md-6">
                        <div class="search-box">
                            <div class="input-group">
                                <span class="input-group-text">
                                    <i class="bi bi-search"></i>
                                </span>
                                <input type="text" 
                                       class="form-control" 
                                       id="contractSearch" 
                                       placeholder="Cerca contratti..."
                                       aria-label="Cerca contratti">
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="d-flex justify-content-md-end gap-2">
                            <select class="form-select form-select-sm" 
                                    id="statusFilter" 
                                    style="width: auto;"
                                    aria-label="Filtra per stato">
                                <option value="">Tutti gli stati</option>
                                <option value="active">Attivo</option>
                                <option value="pending">In attesa</option>
                                <option value="expired">Scaduto</option>
                                <option value="draft">Bozza</option>
                                <option value="terminated">Terminato</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Insert controls before the table
        this.elements.container.insertAdjacentHTML('afterbegin', controlsHtml);
        
        // Bind search events
        this.bindSearchEvents();
    }
    
    /**
     * Bind search and filter events
     */
    bindSearchEvents() {
        const searchInput = document.getElementById('contractSearch');
        const statusFilter = document.getElementById('statusFilter');
        
        if (searchInput) {
            // Debounced search
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.handleSearch(e.target.value);
                }, this.config.searchDelay);
            });
        }
        
        if (statusFilter) {
            statusFilter.addEventListener('change', (e) => {
                this.handleStatusFilter(e.target.value);
            });
        }
    }
    
    /**
     * Handle search functionality
     * @param {string} searchTerm - Search term
     */
    handleSearch(searchTerm) {
        this.state.searchTerm = searchTerm.toLowerCase();
        this.filterRows();
        
        // Announce search results to screen readers
        this.announceSearchResults();
    }
    
    /**
     * Handle status filter
     * @param {string} status - Status to filter by
     */
    handleStatusFilter(status) {
        this.state.statusFilter = status;
        this.filterRows();
    }
    
    /**
     * Filter table rows based on search and filters
     */
    filterRows() {
        let visibleCount = 0;
        
        this.elements.rows.forEach(row => {
            const shouldShow = this.shouldShowRow(row);
            
            if (shouldShow) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });
        
        // Handle empty state
        this.handleEmptyState(visibleCount === 0);
    }
    
    /**
     * Determine if row should be visible
     * @param {HTMLElement} row - Table row
     * @returns {boolean} Should show row
     */
    shouldShowRow(row) {
        const searchTerm = this.state.searchTerm;
        const statusFilter = this.state.statusFilter;
        
        // Search filter
        if (searchTerm) {
            const rowText = row.textContent.toLowerCase();
            if (!rowText.includes(searchTerm)) {
                return false;
            }
        }
        
        // Status filter
        if (statusFilter) {
            const statusBadge = row.querySelector('.status-badge');
            const rowStatus = statusBadge?.className?.match(/status-(\w+)/)?.[1];
            if (rowStatus !== statusFilter) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Handle empty state display
     * @param {boolean} isEmpty - Whether to show empty state
     */
    handleEmptyState(isEmpty) {
        let emptyRow = this.elements.tbody.querySelector('.empty-row');
        
        if (isEmpty && !emptyRow) {
            // Create temporary empty row for filtered results
            const emptyHtml = `
                <tr class="empty-row filtered-empty">
                    <td colspan="6">
                        <div class="empty-contracts-state">
                            <i class="bi bi-search"></i>
                            <h3>Nessun risultato trovato</h3>
                            <p>Prova a modificare i criteri di ricerca.</p>
                        </div>
                    </td>
                </tr>
            `;
            this.elements.tbody.insertAdjacentHTML('beforeend', emptyHtml);
        } else if (!isEmpty && emptyRow?.classList.contains('filtered-empty')) {
            emptyRow.remove();
        }
    }
    
    /**
     * Enhanced delete confirmation with better UX
     */
    bindDeleteConfirmation() {
        const deleteButtons = document.querySelectorAll('.btn-delete');
        
        deleteButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                this.handleDeleteConfirmation(button);
            });
        });
    }
    
    /**
     * Handle delete confirmation with modern modal approach
     * @param {HTMLElement} button - Delete button
     */
    handleDeleteConfirmation(button) {
        const contractSubject = button.getAttribute('data-contract-subject') || 'questo contratto';
        const form = button.closest('form');
        
        // Create confirmation dialog
        const confirmed = confirm(
            `Sei sicuro di voler eliminare "${contractSubject}"?\n\nQuesta azione non può essere annullata.`
        );
        
        if (confirmed) {
            this.showDeleteLoading(button);
            form.submit();
        }
    }
    
    /**
     * Show loading state on delete button
     * @param {HTMLElement} button - Delete button
     */
    showDeleteLoading(button) {
        const originalContent = button.innerHTML;
        button.innerHTML = '<i class="bi bi-hourglass-split"></i>';
        button.disabled = true;
        
        // Restore after timeout (fallback)
        setTimeout(() => {
            button.innerHTML = originalContent;
            button.disabled = false;
        }, 5000);
    }
    
    /**
     * Show/hide table loading state
     * @param {boolean} show - Whether to show loading
     */
    showLoading(show) {
        if (show) {
            this.elements.container.classList.add('table-loading');
        } else {
            this.elements.container.classList.remove('table-loading');
        }
    }
    
    /**
     * Enhance table with additional features
     */
    enhanceTable() {
        // Add hover effects enhancement
        this.enhanceHoverEffects();
        
        // Add keyboard navigation
        this.enhanceKeyboardNavigation();
        
        // Add tooltip for truncated content
        this.addContentTooltips();
    }
    
    /**
     * Enhance hover effects with additional visual feedback
     */
    enhanceHoverEffects() {
        this.elements.rows.forEach(row => {
            row.addEventListener('mouseenter', () => {
                // Add subtle animation class
                row.classList.add('hovered');
            });
            
            row.addEventListener('mouseleave', () => {
                row.classList.remove('hovered');
            });
        });
    }
    
    /**
     * Add keyboard navigation support
     */
    enhanceKeyboardNavigation() {
        // Make table rows focusable for keyboard navigation
        this.elements.rows.forEach((row, index) => {
            row.setAttribute('tabindex', '-1');
            
            row.addEventListener('keydown', (e) => {
                this.handleRowKeyboard(e, index);
            });
        });
    }
    
    /**
     * Handle keyboard navigation in table rows
     * @param {KeyboardEvent} event - Keyboard event
     * @param {number} rowIndex - Current row index
     */
    handleRowKeyboard(event, rowIndex) {
        const { key } = event;
        
        switch (key) {
            case 'ArrowDown':
                event.preventDefault();
                this.focusRow(rowIndex + 1);
                break;
            case 'ArrowUp':
                event.preventDefault();
                this.focusRow(rowIndex - 1);
                break;
            case 'Enter':
                // Focus first action button in row
                const firstAction = event.target.querySelector('.action-btn');
                if (firstAction) {
                    firstAction.focus();
                }
                break;
        }
    }
    
    /**
     * Focus specific row
     * @param {number} index - Row index to focus
     */
    focusRow(index) {
        const visibleRows = Array.from(this.elements.rows).filter(row => 
            row.style.display !== 'none'
        );
        
        if (index >= 0 && index < visibleRows.length) {
            visibleRows[index].focus();
        }
    }
    
    /**
     * Add tooltips for truncated content
     */
    addContentTooltips() {
        const subjectCells = document.querySelectorAll('.contract-subject');
        
        subjectCells.forEach(cell => {
            // Check if content is truncated
            if (cell.scrollWidth > cell.clientWidth) {
                cell.setAttribute('title', cell.textContent);
                cell.style.cursor = 'help';
            }
        });
    }
    
    /**
     * Setup accessibility enhancements
     */
    setupAccessibility() {
        // Add table description
        if (this.elements.table) {
            this.elements.table.setAttribute('aria-label', 'Tabella contratti fornitori');
            
            // Add live region for announcements
            this.createLiveRegion();
        }
    }
    
    /**
     * Create ARIA live region for announcements
     */
    createLiveRegion() {
        const liveRegion = document.createElement('div');
        liveRegion.setAttribute('aria-live', 'polite');
        liveRegion.setAttribute('aria-atomic', 'true');
        liveRegion.className = 'sr-only';
        liveRegion.id = 'contractTableAnnouncements';
        
        document.body.appendChild(liveRegion);
        this.elements.liveRegion = liveRegion;
    }
    
    /**
     * Announce sort change to screen readers
     * @param {string} column - Column name
     * @param {string} direction - Sort direction
     */
    announceSort(column, direction) {
        if (!this.elements.liveRegion) return;
        
        const columnLabels = {
            subject: 'Oggetto',
            supplier: 'Fornitore',
            project: 'Commessa',
            status: 'Stato',
            amount: 'Importo'
        };
        
        const directionLabel = direction === 'asc' ? 'crescente' : 'decrescente';
        const message = `Tabella ordinata per ${columnLabels[column]} in ordine ${directionLabel}`;
        
        this.elements.liveRegion.textContent = message;
    }
    
    /**
     * Announce search results to screen readers
     */
    announceSearchResults() {
        if (!this.elements.liveRegion) return;
        
        const visibleRows = Array.from(this.elements.rows).filter(row => 
            row.style.display !== 'none'
        ).length;
        
        let message;
        if (visibleRows === 0) {
            message = 'Nessun contratto trovato per la ricerca corrente';
        } else if (visibleRows === 1) {
            message = '1 contratto trovato';
        } else {
            message = `${visibleRows} contratti trovati`;
        }
        
        this.elements.liveRegion.textContent = message;
    }
}

/**
 * Initialize when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize contract table manager
    window.contractTableManager = new ContractTableManager();
    
    // Additional Bootstrap enhancements
    initializeBootstrapEnhancements();
});

/**
 * Additional Bootstrap component enhancements
 */
function initializeBootstrapEnhancements() {
    // Initialize tooltips if Bootstrap tooltip is available
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
    
    // Enhance dropdown behavior
    const dropdowns = document.querySelectorAll('.dropdown-toggle');
    dropdowns.forEach(dropdown => {
        dropdown.addEventListener('shown.bs.dropdown', function() {
            // Focus first menu item when dropdown opens
            const firstMenuItem = this.nextElementSibling?.querySelector('.dropdown-item');
            if (firstMenuItem) {
                setTimeout(() => firstMenuItem.focus(), 100);
            }
        });
    });
}