/**
 * EMPLOYMENT LIST ENHANCED JAVASCRIPT
 * 
 * Modern JavaScript module for managing employment list interactions
 * Integrates with existing sort-table.js functionality and adds enhanced UX features
 * 
 * Features:
 * - Real-time search with debouncing
 * - Enhanced table sorting with visual feedback
 * - Filter management with persistence
 * - Toast notifications for user feedback
 * - Loading states and smooth animations
 * - Accessibility improvements
 * - Mobile-responsive interactions
 * 
 * @author Senior UX/UI Developer
 * @version 2.0
 * @since 2025
 */

(function() {
    'use strict';

    // ===== CONFIGURATION & CONSTANTS =====
    const CONFIG = {
        SEARCH_DEBOUNCE_DELAY: 300,        // ms to wait before triggering search
        TOAST_AUTO_HIDE_DELAY: 5000,      // ms before auto-hiding toasts
        ANIMATION_DURATION: 300,           // ms for smooth animations
        STORAGE_PREFIX: 'employment_list_', // localStorage prefix for settings
        BREAKPOINTS: {
            MOBILE: 768,
            TABLET: 1024
        }
    };

    // ===== STATE MANAGEMENT =====
    let state = {
        searchTimeout: null,
        currentFilters: {
            keyword: '',
            status: ''
        },
        isLoading: false,
        currentSort: {
            column: null,
            direction: 'asc'
        },
        isMobile: window.innerWidth <= CONFIG.BREAKPOINTS.MOBILE
    };

    // ===== DOM ELEMENT CACHE =====
    let elements = {};

    /**
     * Initialize the employment list functionality
     * Sets up event listeners, loads saved preferences, and initializes components
     */
    function initializeEmploymentList() {
        console.log('🚀 Initializing Employment List Enhanced Features...');
        
        // Cache DOM elements for better performance
        cacheElements();
        
        // Load user preferences from localStorage
        loadUserPreferences();
        
        // Initialize all components
        initializeFilters();
        initializeSearch();
        initializeTable();
        initializeResponsive();
        initializeAccessibility();
        
		initializeStatusBadges();
		
        // Set up global event listeners
        setupEventListeners();
        
        console.log('✅ Employment List initialization complete');
    }

    /**
     * Cache frequently used DOM elements to avoid repeated queries
     * Improves performance by reducing DOM lookups
     */
    function cacheElements() {
        elements = {
            // Filter elements
            filterToggle: document.getElementById('filterToggle'),
            filterContent: document.getElementById('filterContent'),
            filterForm: document.querySelector('.filter-form'),
            searchInput: document.getElementById('searchInput'),
            statusFilter: document.getElementById('statusFilter'),
            
            // Table elements
            table: document.getElementById('employmentsTable'),
            tableBody: document.querySelector('#employmentsTable tbody'),
            sortableHeaders: document.querySelectorAll('.sortable'),
            
            // Action elements
            deleteModal: document.getElementById('deleteModal'),
            deleteForm: document.getElementById('deleteForm'),
            employmentToDelete: document.getElementById('employmentToDelete'),
            
            // UI feedback elements
            resultsCount: document.querySelector('.results-count'),
            loadingOverlay: document.querySelector('.loading-overlay'),
            
            // Toast container (create if doesn't exist)
            toastContainer: document.querySelector('.toast-container') || createToastContainer()
        };

        console.log('📋 DOM elements cached successfully');
    }

    /**
     * Create toast container for notifications if it doesn't exist
     * @returns {HTMLElement} The created toast container
     */
    function createToastContainer() {
        const container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
        return container;
    }

    /**
     * Initialize filter functionality with smooth animations
     * Handles filter toggle, form submission, and state persistence
     */
    function initializeFilters() {
        if (!elements.filterToggle || !elements.filterContent) return;

        // Enhanced filter toggle with animation
        elements.filterToggle.addEventListener('click', function() {
            const isHidden = elements.filterContent.style.display === 'none' || 
                           !elements.filterContent.classList.contains('show');
            
            toggleFilters(isHidden);
            saveUserPreference('filtersExpanded', isHidden);
        });

        // Auto-submit form on filter changes with debounce
        if (elements.statusFilter) {
            elements.statusFilter.addEventListener('change', function() {
                state.currentFilters.status = this.value;
                debounceFormSubmit();
                saveFiltersToStorage();
            });
        }

        console.log('🔍 Filter functionality initialized');
    }

    /**
     * Toggle filter section with smooth animation
     * @param {boolean} show - Whether to show or hide filters
     */
    function toggleFilters(show) {
        const icon = elements.filterToggle.querySelector('.filter-toggle-btn i');
        
        if (show) {
            elements.filterContent.style.display = 'block';
            // Force reflow for animation
            elements.filterContent.offsetHeight;
            elements.filterContent.classList.add('show');
            icon.className = 'bi bi-chevron-up';
            elements.filterToggle.classList.add('expanded');
        } else {
            elements.filterContent.classList.remove('show');
            icon.className = 'bi bi-chevron-down';
            elements.filterToggle.classList.remove('expanded');
            
            // Hide after animation completes
            setTimeout(() => {
                if (!elements.filterContent.classList.contains('show')) {
                    elements.filterContent.style.display = 'none';
                }
            }, CONFIG.ANIMATION_DURATION);
        }
    }

    /**
     * Initialize enhanced search functionality
     * Includes real-time search with debouncing and visual feedback
     */
    function initializeSearch() {
        if (!elements.searchInput) return;

        // Real-time search with debouncing
        elements.searchInput.addEventListener('input', function() {
            const keyword = this.value.trim();
            state.currentFilters.keyword = keyword;
            
            // Clear previous timeout
            if (state.searchTimeout) {
                clearTimeout(state.searchTimeout);
            }

            // Add visual feedback for typing
            this.classList.add('searching');
            
            // Debounced search
            state.searchTimeout = setTimeout(() => {
                performSearch(keyword);
                this.classList.remove('searching');
            }, CONFIG.SEARCH_DEBOUNCE_DELAY);
        });

        // Clear search button functionality
        const clearButton = createClearSearchButton();
        elements.searchInput.parentNode.appendChild(clearButton);

        console.log('🔎 Enhanced search functionality initialized');
    }

    /**
     * Create a clear search button for better UX
     * @returns {HTMLElement} The clear button element
     */
    function createClearSearchButton() {
        const clearBtn = document.createElement('button');
        clearBtn.type = 'button';
        clearBtn.className = 'search-clear-btn';
        clearBtn.innerHTML = '<i class="bi bi-x-circle"></i>';
        clearBtn.style.cssText = `
            position: absolute;
            right: 2.5rem;
            top: 50%;
            transform: translateY(-50%);
            background: none;
            border: none;
            color: var(--gray-400);
            cursor: pointer;
            padding: 0.25rem;
            border-radius: var(--radius-sm);
            opacity: 0;
            transition: var(--transition-fast);
        `;
        
        clearBtn.addEventListener('click', clearSearch);
        
        // Show/hide based on input content
        elements.searchInput.addEventListener('input', function() {
            clearBtn.style.opacity = this.value ? '1' : '0';
        });
        
        return clearBtn;
    }

    /**
     * Clear the search input and refresh results
     */
    function clearSearch() {
        elements.searchInput.value = '';
        elements.searchInput.dispatchEvent(new Event('input'));
        elements.searchInput.focus();
        showToast('Search cleared', 'success');
    }

    /**
     * Perform search operation with loading feedback
     * @param {string} keyword - The search keyword
     */
    function performSearch(keyword) {
        // For now, we'll work with the existing form submission
        // In a real implementation, this would make an AJAX call
        console.log(`🔍 Searching for: "${keyword}"`);
        
        // Update URL parameters for bookmarkable searches
        updateURLParameters();
        
        // In a real implementation, you would make an AJAX call here
        // For now, we'll just submit the form
        debounceFormSubmit();
    }

    /**
     * Initialize enhanced table functionality
     * Only adds visual enhancements, lets sort-table.js handle sorting
     */
    function initializeTable() {
        if (!elements.table) return;

        // Just enhance visual feedback, don't interfere with sorting
        enhanceTableRows();
        
        // Add visual feedback to sortable headers WITHOUT adding event listeners
        enhanceSortableHeadersVisual();
		
		// Persist and restore sort preferences without interfering with sort-table.js
        setupSortPersistence();
        restoreSortState();
        
        console.log('📊 Enhanced table functionality initialized (visual only)');
    }

    /**
     * Enhance sortable headers with ONLY visual feedback
     * Does NOT add click listeners - leaves that to sort-table.js
     */
    function enhanceSortableHeadersVisual() {
        elements.sortableHeaders.forEach(header => {
            // Only add keyboard support for accessibility, NO click handlers
            header.setAttribute('tabindex', '0');
            header.setAttribute('role', 'button');
            header.setAttribute('aria-label', `Sort by ${header.textContent.trim()}`);
            
            // Only keyboard navigation, no click interference
            header.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    // Let the existing click handler from sort-table.js handle this
                    this.click();
                }
            });
        });
    }

    /**
     * Update visual sort indicators based on current sort state
     */
    function updateSortIndicators() {
        // This works with the existing sort-table.js data attributes
        const table = elements.table;
        const currentCol = table.getAttribute('data-sort-col');
        const currentDir = table.getAttribute('data-sort-dir');
        
        elements.sortableHeaders.forEach((header, index) => {
            header.classList.remove('active');
            header.removeAttribute('data-sort-dir');
            
            if (currentCol == index) {
                header.classList.add('active');
                header.setAttribute('data-sort-dir', currentDir);
                header.setAttribute('aria-label', 
                    `Sort by ${header.textContent.trim()} - currently ${currentDir}ending`);
            }
        });
    }

    /**
     * Enhance table rows with smooth interactions
     */
    function enhanceTableRows() {
        const rows = elements.tableBody?.querySelectorAll('.table-row');
        if (!rows) return;

        rows.forEach(row => {
            // Add staggered animation on load
            row.style.animationDelay = `${Array.from(rows).indexOf(row) * 50}ms`;
            row.classList.add('fade-in');

            // Enhanced hover effects are handled by CSS
            // Add click-to-expand functionality for mobile
            if (state.isMobile) {
                row.addEventListener('click', function(e) {
                    if (!e.target.closest('.action-btn')) {
                        toggleRowDetails(this);
                    }
                });
            }
        });
    }

    /**
     * Toggle detailed view for mobile rows
     * @param {HTMLElement} row - The table row element
     */
    function toggleRowDetails(row) {
        // This would be implemented if we had collapsible row details
        row.classList.toggle('expanded');
        console.log('📱 Mobile row details toggled');
    }

    /**
     * Initialize responsive behavior
     * Handles screen size changes and mobile optimizations
     */
    function initializeResponsive() {
        // Handle window resize
        window.addEventListener('resize', debounce(() => {
            const wasMobile = state.isMobile;
            state.isMobile = window.innerWidth <= CONFIG.BREAKPOINTS.MOBILE;
            
            if (wasMobile !== state.isMobile) {
                handleBreakpointChange();
            }
        }, 250));

        console.log('📱 Responsive functionality initialized');
    }

    /**
     * Handle changes in screen breakpoints
     */
    function handleBreakpointChange() {
        console.log(`📱 Breakpoint changed - Mobile: ${state.isMobile}`);
        
        // Reinitialize mobile-specific features
        if (state.isMobile) {
            // Collapse filters by default on mobile
            if (elements.filterContent?.classList.contains('show')) {
                toggleFilters(false);
            }
        }
    }

    /**
     * Initialize accessibility enhancements
     * Improves keyboard navigation and screen reader support
     */
    function initializeAccessibility() {
        // Add skip link for table navigation
        addSkipLink();
        
        // Enhance ARIA labels and roles
        enhanceARIALabels();
        
        // Add keyboard shortcuts
        addKeyboardShortcuts();
        
        console.log('♿ Accessibility enhancements initialized');
    }

    /**
     * Add skip link for better keyboard navigation
     */
    function addSkipLink() {
        const skipLink = document.createElement('a');
        skipLink.href = '#employmentsTable';
        skipLink.textContent = 'Skip to employment table';
        skipLink.className = 'skip-link';
        skipLink.style.cssText = `
            position: absolute;
            left: -9999px;
            top: 0;
            z-index: 999;
            padding: 0.5rem 1rem;
            background: var(--primary-500);
            color: white;
            text-decoration: none;
            border-radius: var(--radius-md);
        `;
        
        skipLink.addEventListener('focus', function() {
            this.style.left = '1rem';
        });
        
        skipLink.addEventListener('blur', function() {
            this.style.left = '-9999px';
        });
        
        document.body.insertBefore(skipLink, document.body.firstChild);
    }

    /**
     * Enhance ARIA labels for better screen reader support
     */
    function enhanceARIALabels() {
        // Add ARIA label to table
        if (elements.table) {
            elements.table.setAttribute('aria-label', 'Employment relationships table');
            elements.table.setAttribute('role', 'table');
        }

        // Add ARIA labels to filter controls
        if (elements.searchInput) {
            elements.searchInput.setAttribute('aria-label', 'Search employees');
        }
        
        if (elements.statusFilter) {
            elements.statusFilter.setAttribute('aria-label', 'Filter by employment status');
        }
    }

    /**
     * Add keyboard shortcuts for power users
     */
    function addKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            // Ctrl/Cmd + K to focus search
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                elements.searchInput?.focus();
                showToast('Search focused', 'success');
            }
            
            // Ctrl/Cmd + F to toggle filters
            if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
                e.preventDefault();
                elements.filterToggle?.click();
            }
            
            // Escape to clear search
            if (e.key === 'Escape' && document.activeElement === elements.searchInput) {
                clearSearch();
            }
        });
    }

	/**
	     * Update status badges based on end dates
	     * Applies warning or terminated styles and labels
	     */
	    function initializeStatusBadges() {
	        const rows = elements.tableBody?.querySelectorAll('tr') || [];
	        const today = new Date();

	        rows.forEach(row => {
	            const endDateStr = row.dataset.endDate;
	            const badge = row.querySelector('.status-badge');
	            if (!endDateStr || !badge) return;

	            const endDate = new Date(endDateStr);
	            const diffDays = Math.ceil((endDate - today) / (1000 * 60 * 60 * 24));

	            if (diffDays < 0) {
	                badge.classList.remove('status-active', 'status-inactive');
	                badge.classList.add('status-terminated');
	                badge.textContent = 'Cessato';
	            } else if (diffDays <= 30) {
	                badge.classList.remove('status-active', 'status-inactive');
	                badge.classList.add('status-warning');
	                badge.textContent = 'In Scadenza';
	            }
	        });
	    }
	
    /**
     * Set up global event listeners
     */
    function setupEventListeners() {
        // Handle form submissions with loading states
        if (elements.filterForm) {
            elements.filterForm.addEventListener('submit', function(e) {
                showLoading(true);
                // Form will submit normally, loading will be hidden on page load
            });
        }

        // Handle delete confirmations
        setupDeleteHandlers();
        
        console.log('👂 Global event listeners set up');
    }

    /**
     * Set up delete confirmation handlers
     */
    function setupDeleteHandlers() {
        // Enhanced delete button handling
        document.addEventListener('click', function(e) {
            if (e.target.closest('.action-delete')) {
                e.preventDefault();
                const button = e.target.closest('.action-delete');
                const employmentId = button.dataset.employmentId;
                const employeeName = button.dataset.employeeName;
                
                showDeleteConfirmation(employmentId, employeeName);
            }
        });
    }

    /**
     * Show enhanced delete confirmation modal
     * @param {string} employmentId - The employment ID to delete
     * @param {string} employeeName - The employee name for confirmation
     */
    function showDeleteConfirmation(employmentId, employeeName) {
        if (elements.employmentToDelete) {
            elements.employmentToDelete.textContent = employeeName;
        }
        
        if (elements.deleteForm) {
            elements.deleteForm.action = `/fleet/employments/${employmentId}/delete`;
        }
        
        // Show modal with Bootstrap
        if (elements.deleteModal && window.bootstrap) {
            const modal = new bootstrap.Modal(elements.deleteModal);
            modal.show();
            
            // Focus on cancel button for accessibility
            elements.deleteModal.addEventListener('shown.bs.modal', function() {
                this.querySelector('.btn-secondary')?.focus();
            });
        }
        
        console.log(`🗑️ Delete confirmation shown for: ${employeeName}`);
    }

    /**
     * Show loading state with overlay
     * @param {boolean} show - Whether to show or hide loading
     */
    function showLoading(show) {
        if (!elements.loadingOverlay) return;
        
        state.isLoading = show;
        
        if (show) {
            elements.loadingOverlay.classList.add('show');
            elements.loadingOverlay.style.display = 'flex';
        } else {
            elements.loadingOverlay.classList.remove('show');
            setTimeout(() => {
                if (!state.isLoading) {
                    elements.loadingOverlay.style.display = 'none';
                }
            }, CONFIG.ANIMATION_DURATION);
        }
    }

    /**
     * Show toast notification to user
     * @param {string} message - The message to display
     * @param {string} type - The type of toast (success, error, warning, info)
     * @param {number} duration - How long to show the toast (optional)
     */
    function showToast(message, type = 'info', duration = CONFIG.TOAST_AUTO_HIDE_DELAY) {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div class="toast-content">
                <i class="bi bi-${getToastIcon(type)}"></i>
                <span>${message}</span>
            </div>
            <button class="toast-close" aria-label="Close">
                <i class="bi bi-x"></i>
            </button>
        `;
        
        // Add to container
        elements.toastContainer.appendChild(toast);
        
        // Show with animation
        setTimeout(() => toast.classList.add('show'), 10);
        
        // Auto-hide
        const hideTimeout = setTimeout(() => hideToast(toast), duration);
        
        // Manual close button
        toast.querySelector('.toast-close').addEventListener('click', () => {
            clearTimeout(hideTimeout);
            hideToast(toast);
        });
        
        console.log(`🍞 Toast shown: ${message} (${type})`);
    }

    /**
     * Get appropriate icon for toast type
     * @param {string} type - The toast type
     * @returns {string} The icon class
     */
    function getToastIcon(type) {
        const icons = {
            success: 'check-circle',
            error: 'x-circle',
            warning: 'exclamation-triangle',
            info: 'info-circle'
        };
        return icons[type] || icons.info;
    }

    /**
     * Hide toast with animation
     * @param {HTMLElement} toast - The toast element to hide
     */
    function hideToast(toast) {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, CONFIG.ANIMATION_DURATION);
    }

    /**
     * Debounce utility function
     * @param {Function} func - Function to debounce
     * @param {number} wait - Wait time in milliseconds
     * @returns {Function} Debounced function
     */
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func.apply(this, args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * Debounced form submission
     */
    const debounceFormSubmit = debounce(() => {
        if (elements.filterForm) {
            // In a real implementation, this would be an AJAX call
            console.log('🔄 Form submission debounced');
            // elements.filterForm.submit();
        }
    }, CONFIG.SEARCH_DEBOUNCE_DELAY);

    /**
     * Update URL parameters for bookmarkable state
     */
    function updateURLParameters() {
        const url = new URL(window.location);
        
        if (state.currentFilters.keyword) {
            url.searchParams.set('keyword', state.currentFilters.keyword);
        } else {
            url.searchParams.delete('keyword');
        }
        
        if (state.currentFilters.status) {
            url.searchParams.set('status', state.currentFilters.status);
        } else {
            url.searchParams.delete('status');
        }
        
        // Update URL without page reload
        window.history.replaceState({}, '', url);
    }

    /**
     * Save user preferences to localStorage
     * @param {string} key - The preference key
     * @param {*} value - The preference value
     */
    function saveUserPreference(key, value) {
        try {
            localStorage.setItem(CONFIG.STORAGE_PREFIX + key, JSON.stringify(value));
        } catch (e) {
            console.warn('Could not save user preference:', e);
        }
    }
	
	/**
	    * Remove a stored user preference
	    * @param {string} key - The preference key to remove
	    */
	   function removeUserPreference(key) {
	       try {
	           localStorage.removeItem(CONFIG.STORAGE_PREFIX + key);
	       } catch (e) {
	           console.warn('Could not remove user preference:', e);
	       }
	   }

	   /**
	    * Retrieve a stored user preference
	    * @param {string} key - The preference key to read
	    * @returns {*} The stored value or null if missing
	    */
	   function getUserPreference(key) {
	       try {
	           const raw = localStorage.getItem(CONFIG.STORAGE_PREFIX + key);
	           return raw ? JSON.parse(raw) : null;
	       } catch (e) {
	           console.warn('Could not read user preference:', e);
	           return null;
	       }
	   }

    /**
     * Load user preferences from localStorage
     */
    function loadUserPreferences() {
        try {
            // Load filters expanded state
            const filtersExpanded = JSON.parse(
                localStorage.getItem(CONFIG.STORAGE_PREFIX + 'filtersExpanded') || 'false'
            );
            
            if (filtersExpanded && elements.filterToggle) {
                setTimeout(() => elements.filterToggle.click(), 100);
            }
            
            console.log('💾 User preferences loaded');
        } catch (e) {
            console.warn('Could not load user preferences:', e);
        }
    }
	
	/**
	     * Attach listeners to persist current sort selection
	     */
	    function setupSortPersistence() {
	        if (!elements.table || !elements.sortableHeaders) return;

	        elements.sortableHeaders.forEach(header => {
	            header.addEventListener('click', () => {
	                window.requestAnimationFrame(persistCurrentSort);
	            });
	        });
	    }

	    /**
	     * Save the current table sort state to localStorage
	     */
	    function persistCurrentSort() {
	        if (!elements.table) return;

	        const column = elements.table.getAttribute('data-sort-col');
	        const direction = elements.table.getAttribute('data-sort-dir');

	        if (column !== null && column !== '') {
	            saveUserPreference('sortState', {
	                column: parseInt(column, 10),
	                direction: direction || 'asc'
	            });
	            console.log(`💾 Sort state saved (column ${column}, ${direction})`);
	        } else {
	            removeUserPreference('sortState');
	        }
	    }

	    /**
	     * Restore the previously saved sort state, if available
	     */
	    function restoreSortState() {
	        if (!elements.table || !elements.sortableHeaders) return;

	        const sortState = getUserPreference('sortState');
	        if (!sortState || sortState.column === undefined) {
	            return;
	        }

	        const columnIndex = parseInt(sortState.column, 10);
	        const direction = sortState.direction || 'asc';
	        const header = elements.sortableHeaders[columnIndex];

	        if (!header) {
	            return;
	        }

	        // Click header to apply stored sort without duplicating logic
	        header.click();
	        if (direction === 'desc') {
	            header.click();
	        }

	        console.log(`🔁 Restored sort state (column ${columnIndex}, ${direction})`);
	    }

    /**
     * Save current filters to localStorage
     */
    function saveFiltersToStorage() {
        saveUserPreference('filters', state.currentFilters);
    }

    // ===== GLOBAL FUNCTIONS FOR EXTERNAL USE =====
    
    /**
     * Global function for delete employment (called from HTML)
     * Maintains backward compatibility while adding enhancements
     * @param {string} id - Employment ID
     * @param {string} employeeName - Employee name
     */
    window.deleteEmployment = function(id, employeeName) {
        showDeleteConfirmation(id, employeeName);
    };

    /**
     * Global function to show toast (for external use)
     * @param {string} message - Message to show
     * @param {string} type - Toast type
     */
    window.showEmploymentToast = showToast;

    // ===== INITIALIZATION =====
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeEmploymentList);
    } else {
        initializeEmploymentList();
    }

    // Hide loading overlay after page load
    window.addEventListener('load', function() {
        setTimeout(() => showLoading(false), 500);
    });

    console.log('📄 Employment List JavaScript module loaded');

})();