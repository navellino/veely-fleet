/**
 * Enhanced Admin Documents Table JavaScript
 * 
 * Features implemented:
 * - Table sorting functionality with document-specific logic
 * - Search and filtering (by type, expiry status, authority)
 * - Enhanced user interactions and accessibility
 * - Document expiry status management
 * - Performance optimizations
 * 
 * Dependencies: Bootstrap 5.3+, Bootstrap Icons
 */

class AdminDocumentTableManager {
    constructor() {
        // Configuration
        this.config = {
            tableSelector: '.admin-documents-table',
            containerSelector: '.admin-documents-table-container',
            searchDelay: 300, // Debounce delay for search
            animationDuration: 150,
            expiryWarningDays: 30 // Days before expiry to show warning
        };
        
        // State management
        this.state = {
            sortColumn: null,
            sortDirection: 'asc',
            searchTerm: '',
            typeFilter: '',
            expiryFilter: '',
            authorityFilter: '',
            currentData: []
        };
        
        // Cache DOM elements
        this.elements = {};
        
        this.init();
    }
    
    /**
     * Initialize the admin document table manager
     */
    init() {
        this.cacheElements();
        this.bindEvents();
        this.enhanceTable();
        this.setupAccessibility();
        this.calculateExpiryStatuses();
        
        console.log('AdminDocumentTableManager initialized');
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
            console.warn('Admin documents table not found');
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
        
        // Delete button confirmation with enhanced UX
        this.bindDeleteConfirmation();
        
        // Add search and filter functionality
        if (this.elements.container) {
            this.addSearchControls();
        }
        
        // Bind expiry status interactions
        this.bindExpiryStatusInteractions();
    }
    
    /**
     * Handle table sorting with document-specific logic
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
     * Get column name from index - specific to admin documents
     * @param {number} index - Column index
     * @returns {string} Column name
     */
    getColumnName(index) {
        const columnNames = ['type', 'number', 'authority', 'expiry', 'responsible'];
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
     * Sort table rows with document-specific logic
     * @param {string} column - Column to sort by
     * @param {string} direction - Sort direction ('asc' or 'desc')
     */
    sortRows(column, direction) {
        const rowsArray = Array.from(this.elements.rows);
        
        rowsArray.sort((a, b) => {
            const aValue = this.getCellValue(a, column);
            const bValue = this.getCellValue(b, column);
            
            let comparison = 0;
            
            // Handle different data types specific to documents
            switch (column) {
                case 'expiry':
                    comparison = this.compareDates(aValue, bValue);
                    break;
                case 'type':
                case 'authority':
                case 'responsible':
                    comparison = this.compareStrings(aValue, bValue);
                    break;
                case 'number':
                    comparison = this.compareDocumentNumbers(aValue, bValue);
                    break;
                default:
                    comparison = this.compareStrings(aValue, bValue);
            }
            
            return direction === 'desc' ? -comparison : comparison;
        });
        
        // Re-append sorted rows with animation
        this.reorderRows(rowsArray);
    }
    
    /**
     * Get cell value for sorting - specific to admin documents
     * @param {HTMLElement} row - Table row
     * @param {string} column - Column name
     * @returns {string} Cell value
     */
    getCellValue(row, column) {
        const columnMap = {
            type: 0,
            number: 1,
            authority: 2,
            expiry: 3,
            responsible: 4
        };
        
        const cellIndex = columnMap[column];
        const cell = row.cells[cellIndex];
        
        if (!cell) return '';
        
        // Special handling for different column types
        switch (column) {
            case 'type':
                return cell.querySelector('.document-type')?.textContent?.trim() || '';
            case 'number':
                return cell.querySelector('.document-number')?.textContent?.trim() || '';
            case 'authority':
                return cell.querySelector('.authority-name')?.textContent?.trim() || '';
            case 'expiry':
                // Get the actual date value for proper sorting
                const dateElement = cell.querySelector('.expiry-date-text, [data-expiry-date]');
                if (dateElement) {
                    return dateElement.getAttribute('data-expiry-date') || 
                           dateElement.textContent?.trim() || '';
                }
                return cell.textContent?.trim() || '';
            case 'responsible':
                return cell.querySelector('.responsible-person')?.textContent?.trim() || '';
            default:
                return cell.textContent?.trim() || '';
        }
    }
    
    /**
     * Compare dates with null handling
     * @param {string} a - First date
     * @param {string} b - Second date
     * @returns {number} Comparison result
     */
    compareDates(a, b) {
        // Handle empty dates (no expiry)
        if (!a && !b) return 0;
        if (!a) return 1; // No expiry comes after dates
        if (!b) return -1;
        
        const dateA = new Date(a);
        const dateB = new Date(b);
        
        // Handle invalid dates
        if (isNaN(dateA.getTime()) && isNaN(dateB.getTime())) return 0;
        if (isNaN(dateA.getTime())) return 1;
        if (isNaN(dateB.getTime())) return -1;
        
        return dateA.getTime() - dateB.getTime();
    }
    
    /**
     * Compare document numbers (alphanumeric with special handling)
     * @param {string} a - First document number
     * @param {string} b - Second document number
     * @returns {number} Comparison result
     */
    compareDocumentNumbers(a, b) {
        // Extract numeric parts if present for smarter sorting
        const numericA = a.match(/\d+/);
        const numericB = b.match(/\d+/);
        
        if (numericA && numericB) {
            const numA = parseInt(numericA[0]);
            const numB = parseInt(numericB[0]);
            if (numA !== numB) {
                return numA - numB;
            }
        }
        
        // Fall back to string comparison
        return this.compareStrings(a, b);
    }
    
    /**
     * Compare string values
     * @param {string} a - First value
     * @param {string} b - Second value
     * @returns {number} Comparison result
     */
    compareStrings(a, b) {
        return a.localeCompare(b, 'it', { sensitivity: 'base', numeric: true });
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
     * Add search and filter controls specific to documents
     */
    addSearchControls() {
        // Create controls container with document-specific filters
        const controlsHtml = `
            <div class="table-controls">
                <div class="row align-items-center g-3">
                    <div class="col-md-4">
                        <div class="search-box">
                            <div class="input-group">
                                <span class="input-group-text">
                                    <i class="bi bi-search"></i>
                                </span>
                                <input type="text" 
                                       class="form-control" 
                                       id="documentSearch" 
                                       placeholder="Cerca documenti..."
                                       aria-label="Cerca documenti">
                            </div>
                        </div>
                    </div>
                    <div class="col-md-8">
                        <div class="d-flex justify-content-md-end gap-2 flex-wrap">
                            <select class="form-select form-select-sm" 
                                    id="typeFilter" 
                                    style="width: auto; min-width: 120px;"
                                    aria-label="Filtra per tipo">
                                <option value="">Tutti i tipi</option>
                                <option value="license">Licenze</option>
                                <option value="certificate">Certificati</option>
                                <option value="permit">Permessi</option>
                                <option value="insurance">Assicurazioni</option>
                                <option value="contract">Contratti</option>
                                <option value="other">Altri</option>
                            </select>
                            <select class="form-select form-select-sm" 
                                    id="expiryFilter" 
                                    style="width: auto; min-width: 140px;"
                                    aria-label="Filtra per scadenza">
                                <option value="">Tutte le scadenze</option>
                                <option value="valid">Validi</option>
                                <option value="expiring-soon">In scadenza</option>
                                <option value="expired">Scaduti</option>
                                <option value="no-expiry">Senza scadenza</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Insert controls before the table
        this.elements.container.insertAdjacentHTML('afterbegin', controlsHtml);
        
        // Bind search and filter events
        this.bindSearchEvents();
    }
    
    /**
     * Bind search and filter events
     */
    bindSearchEvents() {
        const searchInput = document.getElementById('documentSearch');
        const typeFilter = document.getElementById('typeFilter');
        const expiryFilter = document.getElementById('expiryFilter');
        
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
        
        if (typeFilter) {
            typeFilter.addEventListener('change', (e) => {
                this.handleTypeFilter(e.target.value);
            });
        }
        
        if (expiryFilter) {
            expiryFilter.addEventListener('change', (e) => {
                this.handleExpiryFilter(e.target.value);
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
        this.announceSearchResults();
    }
    
    /**
     * Handle document type filter
     * @param {string} type - Document type to filter by
     */
    handleTypeFilter(type) {
        this.state.typeFilter = type;
        this.filterRows();
    }
    
    /**
     * Handle expiry status filter
     * @param {string} expiryStatus - Expiry status to filter by
     */
    handleExpiryFilter(expiryStatus) {
        this.state.expiryFilter = expiryStatus;
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
     * Determine if row should be visible based on all filters
     * @param {HTMLElement} row - Table row
     * @returns {boolean} Should show row
     */
    shouldShowRow(row) {
        const searchTerm = this.state.searchTerm;
        const typeFilter = this.state.typeFilter;
        const expiryFilter = this.state.expiryFilter;
        
        // Search filter (searches across all text content)
        if (searchTerm) {
            const rowText = row.textContent.toLowerCase();
            if (!rowText.includes(searchTerm)) {
                return false;
            }
        }
        
        // Type filter
        if (typeFilter) {
            const typeElement = row.querySelector('.document-type-icon');
            const rowType = typeElement?.className?.match(/\b(license|certificate|permit|insurance|contract|other)\b/)?.[1];
            if (rowType !== typeFilter) {
                return false;
            }
        }
        
        // Expiry filter
        if (expiryFilter) {
            const expiryElement = row.querySelector('.expiry-status');
            const rowExpiryStatus = expiryElement?.className?.match(/\b(valid|expiring-soon|expired|no-expiry)\b/)?.[1];
            if (rowExpiryStatus !== expiryFilter) {
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
                        <div class="empty-documents-state">
                            <i class="bi bi-search"></i>
                            <h3>Nessun documento trovato</h3>
                            <p>Prova a modificare i criteri di ricerca o i filtri.</p>
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
	 * Calculate and apply expiry statuses with badges
	 */
	calculateExpiryStatuses() {
	    const today = new Date();
	    const warningDate = new Date();
	    warningDate.setDate(today.getDate() + this.config.expiryWarningDays);
	    
	    // Find all rows with expiry dates
	    const expiryElements = document.querySelectorAll('.expiry-status-badge[data-expiry-date]');
	    
	    expiryElements.forEach(element => {
	        const expiryDateStr = element.getAttribute('data-expiry-iso');
	        const expiryDate = new Date(expiryDateStr);
	        
	        if (isNaN(expiryDate.getTime())) {
	            // Invalid date
	            this.setExpiryStatusBadge(element, 'no-expiry', 'Data non valida', null);
	            return;
	        }
	        
	        // Calculate status based on dates
	        let status, statusText, daysLeft;
	        
	        if (expiryDate < today) {
	            daysLeft = Math.abs(Math.ceil((today - expiryDate) / (1000 * 60 * 60 * 24)));
	            status = 'expired';
	            statusText = daysLeft === 1 ? 'Scaduto ieri' : `Scaduto ${daysLeft} giorni fa`;
	        } else if (expiryDate <= warningDate) {
	            daysLeft = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
	            status = 'expiring-soon';
	            if (daysLeft === 0) {
	                statusText = 'Scade oggi';
	            } else if (daysLeft === 1) {
	                statusText = 'Scade domani';
	            } else {
	                statusText = `Scade in ${daysLeft} giorni`;
	            }
	        } else {
	            daysLeft = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
	            status = 'valid';
	            statusText = 'Valido';
	        }
	        
	        this.setExpiryStatusBadge(element, status, statusText, daysLeft);
	    });
	}
    
    
	/**
	 * Set expiry status badge for a document
	 * @param {HTMLElement} element - Badge container element
	 * @param {string} status - Status class
	 * @param {string} statusText - Human readable status
	 * @param {number|null} daysLeft - Days until/since expiry
	 */
	setExpiryStatusBadge(element, status, statusText, daysLeft) {
	    const badgeHtml = `
	        <span class="expiry-status ${status}" title="${statusText}">
	            <i class="bi ${this.getStatusIcon(status)} me-1"></i>
	            ${this.getShortStatusText(status, daysLeft)}
	        </span>
	    `;
	    
	    element.innerHTML = badgeHtml;
	}

	/**
	 * Get short status text for badge
	 * @param {string} status - Status type
	 * @param {number|null} daysLeft - Days until/since expiry
	 * @returns {string} Short status text
	 */
	getShortStatusText(status, daysLeft) {
	    switch (status) {
	        case 'valid':
	            return 'Valido';
	        case 'expiring-soon':
	            if (daysLeft === 0) return 'Oggi';
	            if (daysLeft === 1) return 'Domani';
	            return `${daysLeft}g`;
	        case 'expired':
	            return 'Scaduto';
	        case 'no-expiry':
	            return 'Nessuna';
	        default:
	            return 'N/A';
	    }
	}
	
    
	/**
	 * Get appropriate icon for expiry status
	 * @param {string} status - Status type
	 * @returns {string} Bootstrap icon class
	 */
	getStatusIcon(status) {
	    const icons = {
	        'valid': 'bi-check-circle-fill',
	        'expiring-soon': 'bi-exclamation-triangle-fill',
	        'expired': 'bi-x-circle-fill',
	        'no-expiry': 'bi-infinity'
	    };
	    return icons[status] || 'bi-question-circle';
	}
    
    /**
     * Bind expiry status interactions (tooltips, etc.)
     */
    bindExpiryStatusInteractions() {
        // Add tooltips to expiry status elements
        document.addEventListener('DOMContentLoaded', () => {
            const statusElements = document.querySelectorAll('.expiry-status');
            statusElements.forEach(element => {
                const status = element.className.match(/\b(valid|expiring-soon|expired|no-expiry)\b/)?.[1];
                const tooltips = {
                    'valid': 'Documento valido',
                    'expiring-soon': `Scade entro ${this.config.expiryWarningDays} giorni`,
                    'expired': 'Documento scaduto - richiede attenzione',
                    'no-expiry': 'Documento senza scadenza'
                };
                
                if (tooltips[status]) {
                    element.setAttribute('title', tooltips[status]);
                    element.setAttribute('data-bs-toggle', 'tooltip');
                }
            });
        });
    }
    
    /**
     * Enhanced delete confirmation with document-specific information
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
     * Handle delete confirmation with document details
     * @param {HTMLElement} button - Delete button
     */
    handleDeleteConfirmation(button) {
        const row = button.closest('tr');
        const documentType = row.querySelector('.document-type')?.textContent?.trim() || 'documento';
        const documentNumber = row.querySelector('.document-number')?.textContent?.trim() || '';
        const form = button.closest('form');
        
        const documentInfo = documentNumber ? `${documentType} (${documentNumber})` : documentType;
        
        // Create confirmation dialog
        const confirmed = confirm(
            `Sei sicuro di voler eliminare ${documentInfo}?\n\nQuesta azione non può essere annullata.`
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
        
        // Add content tooltips for truncated text
        this.addContentTooltips();
        
        // Initialize document type icons
        this.initializeDocumentTypeIcons();
    }
    
    /**
     * Initialize document type icons based on content
     */
    initializeDocumentTypeIcons() {
        const typeElements = document.querySelectorAll('.document-type');
        
        typeElements.forEach(element => {
            const typeText = element.textContent?.toLowerCase().trim();
            let iconClass = 'other'; // default
            
            // Determine icon based on document type text
            if (typeText.includes('licen') || typeText.includes('permess')) {
                iconClass = 'license';
            } else if (typeText.includes('certificat') || typeText.includes('attestat')) {
                iconClass = 'certificate';
            } else if (typeText.includes('permess') || typeText.includes('autor')) {
                iconClass = 'permit';
            } else if (typeText.includes('assicur') || typeText.includes('polizz')) {
                iconClass = 'insurance';
            } else if (typeText.includes('contratt')) {
                iconClass = 'contract';
            }
            
            // Add icon if not already present
            if (!element.querySelector('.document-type-icon')) {
                const icon = document.createElement('div');
                icon.className = `document-type-icon ${iconClass}`;
                icon.innerHTML = this.getTypeIcon(iconClass);
                element.prepend(icon);
            }
        });
    }
    
    /**
     * Get icon for document type
     * @param {string} type - Document type
     * @returns {string} Icon HTML
     */
    getTypeIcon(type) {
        const icons = {
            'license': '<i class="bi bi-award"></i>',
            'certificate': '<i class="bi bi-patch-check"></i>',
            'permit': '<i class="bi bi-file-earmark-check"></i>',
            'insurance': '<i class="bi bi-shield-check"></i>',
            'contract': '<i class="bi bi-file-text"></i>',
            'other': '<i class="bi bi-file-earmark"></i>'
        };
        return icons[type] || icons.other;
    }
    
    /**
     * Enhance hover effects with additional visual feedback
     */
    enhanceHoverEffects() {
        this.elements.rows.forEach(row => {
            row.addEventListener('mouseenter', () => {
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
        const truncatableElements = document.querySelectorAll(
            '.document-type, .document-number, .authority-name, .responsible-person'
        );
        
        truncatableElements.forEach(element => {
            // Check if content is truncated
            if (element.scrollWidth > element.clientWidth) {
                element.setAttribute('title', element.textContent);
                element.style.cursor = 'help';
            }
        });
    }
    
    /**
     * Setup accessibility enhancements
     */
    setupAccessibility() {
        // Add table description
        if (this.elements.table) {
            this.elements.table.setAttribute('aria-label', 'Tabella documenti amministrativi');
            
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
        liveRegion.id = 'adminDocumentTableAnnouncements';
        
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
            type: 'Tipo',
            number: 'Numero',
            authority: 'Ente',
            expiry: 'Scadenza',
            responsible: 'Responsabile'
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
			message = 'Nessun documento trovato per la ricerca corrente';
			       } else if (visibleRows === 1) {
			           message = '1 documento trovato';
			       } else {
			           message = `${visibleRows} documenti trovati`;
			       }
			       
			       this.elements.liveRegion.textContent = message;
			   }
			   
			   /**
			    * Get document statistics for dashboard updates
			    * @returns {Object} Document statistics
			    */
			   getDocumentStatistics() {
			       const stats = {
			           total: 0,
			           valid: 0,
			           expiringSoon: 0,
			           expired: 0,
			           noExpiry: 0,
			           byType: {}
			       };
			       
			       this.elements.rows.forEach(row => {
			           stats.total++;
			           
			           // Count by expiry status
			           const expiryStatus = row.querySelector('.expiry-status');
			           if (expiryStatus) {
			               const statusClass = expiryStatus.className;
			               if (statusClass.includes('valid')) stats.valid++;
			               else if (statusClass.includes('expiring-soon')) stats.expiringSoon++;
			               else if (statusClass.includes('expired')) stats.expired++;
			               else if (statusClass.includes('no-expiry')) stats.noExpiry++;
			           }
			           
			           // Count by document type
			           const typeIcon = row.querySelector('.document-type-icon');
			           if (typeIcon) {
			               const typeClass = typeIcon.className;
			               const type = typeClass.match(/\b(license|certificate|permit|insurance|contract|other)\b/)?.[1] || 'other';
			               stats.byType[type] = (stats.byType[type] || 0) + 1;
			           }
			       });
			       
			       return stats;
			   }
			   
			   /**
			    * Export table data to CSV
			    * @param {string} filename - Output filename
			    */
			   exportToCSV(filename = 'documenti_amministrativi.csv') {
			       const headers = ['Tipo', 'Numero', 'Ente', 'Scadenza', 'Stato', 'Responsabile'];
			       const rows = [];
			       
			       // Add headers
			       rows.push(headers.join(','));
			       
			       // Add data rows
			       this.elements.rows.forEach(row => {
			           if (row.style.display === 'none') return; // Skip filtered out rows
			           
			           const cells = row.querySelectorAll('td');
			           const rowData = [];
			           
			           // Extract data from each cell
			           cells.forEach((cell, index) => {
			               if (index === cells.length - 1) return; // Skip actions column
			               
			               let cellText = '';
			               switch (index) {
			                   case 0: // Type
			                       cellText = cell.querySelector('.document-type')?.textContent?.trim() || '';
			                       break;
			                   case 1: // Number
			                       cellText = cell.querySelector('.document-number')?.textContent?.trim() || '';
			                       break;
			                   case 2: // Authority
			                       cellText = cell.querySelector('.authority-name')?.textContent?.trim() || '';
			                       break;
			                   case 3: // Expiry
			                       const dateText = cell.querySelector('.expiry-date-text')?.textContent?.trim();
			                       const statusText = cell.querySelector('.expiry-status')?.textContent?.trim();
			                       cellText = dateText || statusText || '';
			                       break;
			                   case 4: // Responsible
			                       cellText = cell.querySelector('.responsible-person')?.textContent?.trim() || '';
			                       break;
			                   default:
			                       cellText = cell.textContent?.trim() || '';
			               }
			               
			               // Escape CSV special characters
			               cellText = cellText.replace(/"/g, '""');
			               if (cellText.includes(',') || cellText.includes('"') || cellText.includes('\n')) {
			                   cellText = `"${cellText}"`;
			               }
			               
			               rowData.push(cellText);
			           });
			           
			           rows.push(rowData.join(','));
			       });
			       
			       // Create and download file
			       const csvContent = rows.join('\n');
			       const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
			       const link = document.createElement('a');
			       
			       if (link.download !== undefined) {
			           const url = URL.createObjectURL(blob);
			           link.setAttribute('href', url);
			           link.setAttribute('download', filename);
			           link.style.visibility = 'hidden';
			           document.body.appendChild(link);
			           link.click();
			           document.body.removeChild(link);
			       }
			   }
			}

			/**
			* Initialize when DOM is ready
			*/
			document.addEventListener('DOMContentLoaded', function() {
			   // Initialize admin document table manager
			   window.adminDocumentTableManager = new AdminDocumentTableManager();
			   
			   // Additional Bootstrap enhancements
			   initializeBootstrapEnhancements();
			   
			   // Initialize export functionality
			   initializeExportFunctionality();
			   
			   // Initialize periodic expiry checks
			   initializeExpiryMonitoring();
			});

			/**
			* Additional Bootstrap component enhancements specific to admin documents
			*/
			function initializeBootstrapEnhancements() {
			   // Initialize tooltips if Bootstrap tooltip is available
			   if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
			       const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
			       tooltipTriggerList.map(function (tooltipTriggerEl) {
			           return new bootstrap.Tooltip(tooltipTriggerEl);
			       });
			   }
			   
			   // Enhance dropdown behavior for filters
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
			   
			   // Add ripple effect to buttons
			   addRippleEffect();
			}

			/**
			* Initialize export functionality
			*/
			function initializeExportFunctionality() {
			   // Bind export buttons
			   const exportButtons = document.querySelectorAll('[data-export]');
			   exportButtons.forEach(button => {
			       button.addEventListener('click', (e) => {
			           e.preventDefault();
			           const exportType = button.getAttribute('data-export');
			           
			           if (exportType === 'csv' && window.adminDocumentTableManager) {
			               window.adminDocumentTableManager.exportToCSV();
			           }
			       });
			   });
			   
			   // Add keyboard shortcut for export (Ctrl+E)
			   document.addEventListener('keydown', (e) => {
			       if (e.ctrlKey && e.key === 'e') {
			           e.preventDefault();
			           if (window.adminDocumentTableManager) {
			               window.adminDocumentTableManager.exportToCSV();
			           }
			       }
			   });
			}

			/**
			* Initialize expiry monitoring for real-time updates
			*/
			function initializeExpiryMonitoring() {
			   // Check for expiry status updates every hour
			   setInterval(() => {
			       if (window.adminDocumentTableManager) {
			           window.adminDocumentTableManager.calculateExpiryStatuses();
			       }
			   }, 3600000); // 1 hour
			   
			   // Also check when page becomes visible again
			   document.addEventListener('visibilitychange', () => {
			       if (!document.hidden && window.adminDocumentTableManager) {
			           window.adminDocumentTableManager.calculateExpiryStatuses();
			       }
			   });
			}

			/**
			* Add ripple effect to buttons for enhanced UX
			*/
			function addRippleEffect() {
			   const buttons = document.querySelectorAll('.btn, .action-btn');
			   
			   buttons.forEach(button => {
			       button.addEventListener('click', function(e) {
			           // Skip if reduced motion is preferred
			           if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
			               return;
			           }
			           
			           const ripple = document.createElement('span');
			           const rect = this.getBoundingClientRect();
			           const size = Math.max(rect.width, rect.height);
			           const x = e.clientX - rect.left - size / 2;
			           const y = e.clientY - rect.top - size / 2;
			           
			           ripple.style.width = ripple.style.height = size + 'px';
			           ripple.style.left = x + 'px';
			           ripple.style.top = y + 'px';
			           ripple.classList.add('ripple');
			           
			           this.appendChild(ripple);
			           
			           // Remove ripple after animation
			           setTimeout(() => {
			               ripple.remove();
			           }, 600);
			       });
			   });
			}

			/**
			* Utility functions for document management
			*/
			const DocumentUtils = {
			   /**
			    * Format date for display
			    * @param {Date|string} date - Date to format
			    * @returns {string} Formatted date
			    */
			   formatDate(date) {
			       if (!date) return '';
			       const d = new Date(date);
			       return isNaN(d.getTime()) ? '' : d.toLocaleDateString('it-IT');
			   },
			   
			   /**
			    * Calculate days until expiry
			    * @param {Date|string} expiryDate - Expiry date
			    * @returns {number} Days until expiry (negative if expired)
			    */
			   daysUntilExpiry(expiryDate) {
			       if (!expiryDate) return null;
			       const expiry = new Date(expiryDate);
			       const today = new Date();
			       const diffTime = expiry.getTime() - today.getTime();
			       return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
			   },
			   
			   /**
			    * Get human-readable expiry status
			    * @param {Date|string} expiryDate - Expiry date
			    * @param {number} warningDays - Days before expiry to show warning
			    * @returns {Object} Status object with class and text
			    */
			   getExpiryStatus(expiryDate, warningDays = 30) {
			       if (!expiryDate) {
			           return { class: 'no-expiry', text: 'Nessuna scadenza' };
			       }
			       
			       const daysLeft = this.daysUntilExpiry(expiryDate);
			       
			       if (daysLeft < 0) {
			           return { class: 'expired', text: 'Scaduto' };
			       } else if (daysLeft <= warningDays) {
			           return { class: 'expiring-soon', text: 'In scadenza' };
			       } else {
			           return { class: 'valid', text: 'Valido' };
			       }
			   },
			   
			   /**
			    * Validate document number format
			    * @param {string} documentNumber - Document number to validate
			    * @returns {boolean} Is valid
			    */
			   validateDocumentNumber(documentNumber) {
			       if (!documentNumber || documentNumber.trim() === '') return false;
			       // Basic validation - adjust regex based on your document number format
			       return /^[A-Z0-9\-\/]+$/i.test(documentNumber.trim());
			   }
			};

			// Make utilities available globally
			window.DocumentUtils = DocumentUtils;	