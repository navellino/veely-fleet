/**
 * EXPENSE REPORTS ENHANCED JAVASCRIPT
 * 
 * Modern JavaScript module for managing expense reports interactions
 * Integrates with existing sort-table.js functionality and adds enhanced UX features
 * 
 * Features:
 * - Real-time filtering with smooth animations
 * - Enhanced table sorting with visual feedback
 * - Auto-complete employee filter population
 * - Date range filtering with smart defaults
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
        FILTER_DEBOUNCE_DELAY: 300,       // ms to wait before applying filters
        TOAST_AUTO_HIDE_DELAY: 4000,     // ms before auto-hiding toasts
        ANIMATION_DURATION: 300,          // ms for smooth animations
        STORAGE_PREFIX: 'expense_reports_', // localStorage prefix for settings
        BREAKPOINTS: {
            MOBILE: 768,
            TABLET: 1024
        }
    };

    // ===== STATE MANAGEMENT =====
    let state = {
        filterTimeout: null,
        currentFilters: {
            employee: '',
            status: '',
            startDate: '',
            endDate: ''
        },
        isLoading: false,
        originalRows: [],
        visibleCount: 0,
        isMobile: window.innerWidth <= CONFIG.BREAKPOINTS.MOBILE
    };

    // ===== DOM ELEMENT CACHE =====
    let elements = {};

    /**
     * Initialize the expense reports functionality
     * Sets up event listeners, loads saved preferences, and initializes components
     */
    function initializeExpenseReports() {
        console.log('🚀 Initializing Expense Reports Enhanced Features...');
        
        // Cache DOM elements for better performance
        cacheElements();
        
        // Initialize all components
        initializeEmployeeFilter();
        initializeFilters();
        initializeTable();
        initializeResponsive();
        initializeAccessibility();
        
        // Set up global event listeners
        setupEventListeners();
        
        // Set default filters and apply
        setDefaultFilters();
        
        console.log('✅ Expense Reports initialization complete');
    }

    /**
     * Cache frequently used DOM elements to avoid repeated queries
     * Improves performance by reducing DOM lookups
     */
    function cacheElements() {
        elements = {
            // Filter elements
            employeeFilter: document.getElementById('employeeFilter'),
            statusFilter: document.getElementById('statusFilter'),
            startDateFilter: document.getElementById('startDateFilter'),
            endDateFilter: document.getElementById('endDateFilter'),
            applyFiltersBtn: document.querySelector('.btn-apply-filters'),
            
            // Table elements
            table: document.getElementById('expenseTable'),
            tableBody: document.getElementById('expenseTableBody'),
            expenseRows: document.querySelectorAll('.expense-row'),
            
            // UI feedback elements
            visibleCount: document.getElementById('visibleCount'),
            noResults: document.getElementById('noResults'),
            
            // Toast container (create if doesn't exist)
            toastContainer: document.querySelector('.toast-container') || createToastContainer()
        };

        // Store original rows for filtering
        state.originalRows = Array.from(elements.expenseRows);

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
     * Initialize employee filter with dynamic options from table data
     */
    function initializeEmployeeFilter() {
        if (!elements.employeeFilter) return;

        const employees = new Set();
        
        // Extract unique employees from table rows
        state.originalRows.forEach(row => {
            const employee = row.getAttribute('data-employee');
            if (employee && employee.trim()) {
                employees.add(employee.trim());
            }
        });
        
        // Sort employees alphabetically
        const sortedEmployees = Array.from(employees).sort();
        
        // Clear existing options except the first one
        while (elements.employeeFilter.children.length > 1) {
            elements.employeeFilter.removeChild(elements.employeeFilter.lastChild);
        }
        
        // Add employee options
        sortedEmployees.forEach(employee => {
            const option = document.createElement('option');
            option.value = employee;
            option.textContent = employee;
            elements.employeeFilter.appendChild(option);
        });
        
        console.log(`👥 Employee filter initialized with ${sortedEmployees.length} employees`);
    }

    /**
     * Initialize filter functionality with real-time updates
     */
    function initializeFilters() {
        // Add event listeners for real-time filtering
        if (elements.employeeFilter) {
            elements.employeeFilter.addEventListener('change', debouncedApplyFilters);
        }
        
        if (elements.statusFilter) {
            elements.statusFilter.addEventListener('change', debouncedApplyFilters);
        }
        
        if (elements.startDateFilter) {
            elements.startDateFilter.addEventListener('change', debouncedApplyFilters);
        }
        
        if (elements.endDateFilter) {
            elements.endDateFilter.addEventListener('change', debouncedApplyFilters);
        }

        console.log('🔍 Filter functionality initialized');
    }

    /**
     * Set default date filter to current year
     */
    function setDefaultFilters() {
        const currentYear = new Date().getFullYear();
        const startDate = `${currentYear}-01-01`;
        const endDate = `${currentYear}-12-31`;
        
        if (elements.startDateFilter && !elements.startDateFilter.value) {
            elements.startDateFilter.value = startDate;
            state.currentFilters.startDate = startDate;
        }
        
        if (elements.endDateFilter && !elements.endDateFilter.value) {
            elements.endDateFilter.value = endDate;
            state.currentFilters.endDate = endDate;
        }
        
        // Apply initial filters
        applyFilters();
        
        console.log(`📅 Default date filter set to ${currentYear}`);
    }

    /**
     * Initialize enhanced table functionality
     * Adds visual enhancements while preserving sort-table.js functionality
     */
    function initializeTable() {
        if (!elements.table) return;

        // Ensure table has sortable class for sort-table.js
        elements.table.classList.add('sortable');
        
        // Add enhanced row animations
        enhanceTableRows();
        
        console.log('📊 Enhanced table functionality initialized');
    }

    /**
     * Enhance table rows with animations and interactions
     */
    function enhanceTableRows() {
        state.originalRows.forEach((row, index) => {
            // Add staggered animation on load
            row.style.animationDelay = `${index * 50}ms`;
            row.classList.add('fade-in');

            // Add enhanced hover effects (handled by CSS)
            // Add click-to-expand functionality for mobile if needed
            if (state.isMobile) {
                row.addEventListener('click', function(e) {
                    if (!e.target.closest('.action-buttons')) {
                        toggleRowDetails(this);
                    }
                });
            }
        });
    }

    /**
     * Toggle detailed view for mobile rows (placeholder for future enhancement)
     * @param {HTMLElement} row - The table row element
     */
    function toggleRowDetails(row) {
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
        
        // Reinitialize mobile-specific features if needed
        if (state.isMobile) {
            // Add mobile-specific optimizations
            optimizeForMobile();
        }
    }

    /**
     * Optimize interface for mobile devices
     */
    function optimizeForMobile() {
        // Hide less important columns on mobile (handled by CSS)
        // Simplify interactions for touch devices
        console.log('📱 Mobile optimizations applied');
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
        skipLink.href = '#expenseTable';
        skipLink.textContent = 'Skip to expense reports table';
        skipLink.className = 'skip-link';
        skipLink.style.cssText = `
            position: absolute;
            left: -9999px;
            top: 0;
            z-index: 999;
            padding: 0.5rem 1rem;
            background: #667eea;
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
            elements.table.setAttribute('aria-label', 'Expense reports table');
            elements.table.setAttribute('role', 'table');
        }

        // Add ARIA labels to filter controls
        if (elements.employeeFilter) {
            elements.employeeFilter.setAttribute('aria-label', 'Filter by employee');
        }
        
        if (elements.statusFilter) {
            elements.statusFilter.setAttribute('aria-label', 'Filter by status');
        }
        
        if (elements.startDateFilter) {
            elements.startDateFilter.setAttribute('aria-label', 'Filter start date');
        }
        
        if (elements.endDateFilter) {
            elements.endDateFilter.setAttribute('aria-label', 'Filter end date');
        }
    }

    /**
     * Add keyboard shortcuts for power users
     */
    function addKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            // Ctrl/Cmd + F to focus employee filter
            if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
                e.preventDefault();
                elements.employeeFilter?.focus();
                showToast('Employee filter focused', 'success', 2000);
            }
            
            // Ctrl/Cmd + R to reset filters
            if ((e.ctrlKey || e.metaKey) && e.key === 'r') {
                e.preventDefault();
                clearFilters();
                showToast('Filters cleared', 'success', 2000);
            }
        });
    }

    /**
     * Set up global event listeners
     */
    function setupEventListeners() {
        // Apply filters button
        if (elements.applyFiltersBtn) {
            elements.applyFiltersBtn.addEventListener('click', function() {
                applyFilters();
                showToast('Filters applied', 'success', 2000);
            });
        }

        // Enhanced form submission handling
        const forms = document.querySelectorAll('form');
        forms.forEach(form => {
            form.addEventListener('submit', function(e) {
                // Add loading state for delete operations
                if (this.action.includes('/delete')) {
                    showLoading(true);
                }
            });
        });
        
        console.log('👂 Global event listeners set up');
    }

    /**
     * Apply filters to table rows with smooth animations
     */
    function applyFilters() {
        // Get current filter values
        state.currentFilters = {
            employee: elements.employeeFilter?.value.toLowerCase() || '',
            status: elements.statusFilter?.value.toLowerCase() || '',
            startDate: elements.startDateFilter?.value || '',
            endDate: elements.endDateFilter?.value || ''
        };
        
        let visibleCount = 0;
        
        // Filter rows
        state.originalRows.forEach((row, index) => {
            let showRow = true;
            
            // Employee filter
            if (state.currentFilters.employee) {
                const employee = row.getAttribute('data-employee')?.toLowerCase() || '';
                if (!employee.includes(state.currentFilters.employee)) {
                    showRow = false;
                }
            }
            
            // Status filter
            if (state.currentFilters.status) {
                const status = row.getAttribute('data-status')?.toLowerCase() || '';
                if (status !== state.currentFilters.status) {
                    showRow = false;
                }
            }
            
            // Date range filter
            if (state.currentFilters.startDate || state.currentFilters.endDate) {
                const rowStartDate = row.getAttribute('data-start-date');
                const rowEndDate = row.getAttribute('data-end-date');
                
                if (state.currentFilters.startDate && rowStartDate && rowStartDate < state.currentFilters.startDate) {
                    showRow = false;
                }
                
                if (state.currentFilters.endDate && rowEndDate && rowEndDate > state.currentFilters.endDate) {
                    showRow = false;
                }
            }
            
            // Apply visibility with smooth animation
            if (showRow) {
                showRowWithAnimation(row, index);
                visibleCount++;
            } else {
                hideRowWithAnimation(row);
            }
        });
        
        // Update UI
        updateResultsCount(visibleCount);
        toggleNoResultsMessage(visibleCount === 0);
        
        // Save filter state
        saveFiltersToStorage();
        
        console.log(`🔍 Filters applied - ${visibleCount} rows visible`);
    }

    /**
     * Show row with smooth animation
     * @param {HTMLElement} row - The row to show
     * @param {number} index - Row index for staggered animation
     */
    function showRowWithAnimation(row, index) {
        if (row.style.display === 'none') {
            row.style.display = '';
            row.style.opacity = '0';
            row.style.transform = 'translateY(10px)';
            
            setTimeout(() => {
                row.style.transition = 'all 0.3s ease';
                row.style.opacity = '1';
                row.style.transform = 'translateY(0)';
            }, index * 20); // Staggered animation
        }
    }

    /**
     * Hide row with smooth animation
     * @param {HTMLElement} row - The row to hide
     */
    function hideRowWithAnimation(row) {
        if (row.style.display !== 'none') {
            row.style.transition = 'all 0.2s ease';
            row.style.opacity = '0';
            row.style.transform = 'translateY(-10px)';
            
            setTimeout(() => {
                row.style.display = 'none';
            }, 200);
        }
    }

    /**
     * Update visible results count
     * @param {number} count - Number of visible rows
     */
    function updateResultsCount(count) {
        if (elements.visibleCount) {
            elements.visibleCount.textContent = count;
            
            // Add pulse animation for count changes
            elements.visibleCount.style.animation = 'none';
            setTimeout(() => {
                elements.visibleCount.style.animation = 'pulse 0.5s ease';
            }, 10);
        }
        
        state.visibleCount = count;
    }

    /**
     * Toggle no results message visibility
     * @param {boolean} show - Whether to show the no results message
     */
    function toggleNoResultsMessage(show) {
        if (elements.noResults && elements.tableBody) {
            if (show) {
                elements.noResults.style.display = 'block';
                elements.tableBody.style.display = 'none';
            } else {
                elements.noResults.style.display = 'none';
                elements.tableBody.style.display = '';
            }
        }
    }

    /**
     * Clear all filters and reset to defaults
     */
    function clearFilters() {
        if (elements.employeeFilter) elements.employeeFilter.value = '';
        if (elements.statusFilter) elements.statusFilter.value = '';
        if (elements.startDateFilter) elements.startDateFilter.value = '';
        if (elements.endDateFilter) elements.endDateFilter.value = '';
        
        // Reset to default year filter
        setDefaultFilters();
    }

    /**
     * Debounced apply filters function
     */
    const debouncedApplyFilters = debounce(() => {
        applyFilters();
    }, CONFIG.FILTER_DEBOUNCE_DELAY);

    /**
     * Show loading state with overlay
     * @param {boolean} show - Whether to show or hide loading
     */
    function showLoading(show) {
        const loadingOverlay = document.querySelector('.loading-overlay');
        if (!loadingOverlay) return;
        
        state.isLoading = show;
        
        if (show) {
            loadingOverlay.classList.add('show');
            loadingOverlay.style.display = 'flex';
        } else {
            loadingOverlay.classList.remove('show');
            setTimeout(() => {
                if (!state.isLoading) {
                    loadingOverlay.style.display = 'none';
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
     * Save current filters to localStorage
     */
    function saveFiltersToStorage() {
        try {
            localStorage.setItem(CONFIG.STORAGE_PREFIX + 'filters', JSON.stringify(state.currentFilters));
        } catch (e) {
            console.warn('Could not save filters to storage:', e);
        }
    }

    /**
     * Load filters from localStorage
     */
    function loadFiltersFromStorage() {
        try {
            const savedFilters = localStorage.getItem(CONFIG.STORAGE_PREFIX + 'filters');
            if (savedFilters) {
                const filters = JSON.parse(savedFilters);
                
                // Restore filter values (except dates which should default to current year)
                if (elements.employeeFilter && filters.employee) {
                    elements.employeeFilter.value = filters.employee;
                }
                if (elements.statusFilter && filters.status) {
                    elements.statusFilter.value = filters.status;
                }
            }
        } catch (e) {
            console.warn('Could not load filters from storage:', e);
        }
    }

    // ===== GLOBAL FUNCTIONS FOR BACKWARD COMPATIBILITY =====
    
    /**
     * Global function to apply filters (maintains backward compatibility)
     */
    window.applyFilters = function() {
        applyFilters();
    };

    /**
     * Global function to clear filters
     */
    window.clearFilters = function() {
        clearFilters();
        showToast('All filters cleared', 'success', 2000);
    };

    /**
     * Global function to export filtered results
     */
    window.exportFilteredResults = function() {
        const visibleRows = state.originalRows.filter(row => row.style.display !== 'none');
        console.log(`📊 Exporting ${visibleRows.length} filtered results...`);
        showToast(`Exporting ${visibleRows.length} expense reports...`, 'info', 3000);
        // Export logic would be implemented here
    };

    /**
     * Global function to show toast (for external use)
     * @param {string} message - Message to show
     * @param {string} type - Toast type
     */
    window.showExpenseToast = showToast;

    // ===== INITIALIZATION =====
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeExpenseReports);
    } else {
        initializeExpenseReports();
    }

    // Hide loading overlay after page load
    window.addEventListener('load', function() {
        setTimeout(() => showLoading(false), 500);
    });

    console.log('📄 Expense Reports JavaScript module loaded');

})();