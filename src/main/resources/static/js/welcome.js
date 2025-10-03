/**
 * WELCOME PAGE JAVASCRIPT MODULE
 * Modular, accessible and performance-optimized dashboard functionality
 * Author: Senior UX/UI Developer
 * Dependencies: Chart.js, AOS, Bootstrap 5
 */

'use strict';

/**
 * Main Welcome Dashboard Module
 * Handles all dashboard functionality with modular architecture
 */
const WelcomeDashboard = (() => {
    
    // Configuration object for easy maintenance
    const CONFIG = {
        // Chart.js global configuration
        charts: {
            font: {
                family: 'Inter, sans-serif',
                size: 12
            },
            color: '#6b7280',
            responsive: true,
            maintainAspectRatio: false
        },
        
        // AOS animation settings
        animations: {
            duration: 800,
            easing: 'ease-out-cubic',
            once: true,
            offset: 50
        },
        
        // Performance settings
        performance: {
            enableAnimations: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
            enableHover: !window.matchMedia('(hover: none)').matches
        }
    };

    /**
     * Chart Management Module
     * Handles all Chart.js instances with centralized configuration
     */
    const ChartManager = {
        charts: new Map(), // Store chart instances for cleanup
        
        /**
         * Initialize Chart.js global defaults
         * Sets consistent styling across all charts
         */
        initGlobalDefaults() {
            Chart.defaults.font.family = CONFIG.charts.font.family;
            Chart.defaults.font.size = CONFIG.charts.font.size;
            Chart.defaults.color = CONFIG.charts.color;
        },

        /**
         * Create Vehicle Status Doughnut Chart
         * @param {HTMLCanvasElement} canvas - Chart canvas element
         * @param {Array} labels - Chart labels array
         * @param {Array} values - Chart data values
         */
        createVehicleChart(canvas, labels, values) {
            const ctx = canvas.getContext('2d');
            
            const chart = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: values,
                        backgroundColor: [
                            '#3b82f6', // Primary blue
                            '#10b981', // Success green
                            '#f59e0b', // Warning amber
                            '#ef4444'  // Danger red
                        ],
                        borderWidth: 0,
                        cutout: '65%'
                    }]
                },
                options: {
                    ...CONFIG.charts,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                padding: 16,
                                usePointStyle: true,
                                pointStyle: 'circle',
                                font: {
                                    size: 11
                                }
                            }
                        },
                        tooltip: {
                            backgroundColor: 'rgba(0, 0, 0, 0.8)',
                            titleColor: '#ffffff',
                            bodyColor: '#ffffff',
                            borderColor: 'rgba(255, 255, 255, 0.1)',
                            borderWidth: 1,
                            cornerRadius: 8,
                            padding: 12,
                            displayColors: true
                        }
                    },
                    // Accessibility enhancements
                    onHover: (event, elements) => {
                        canvas.style.cursor = elements.length > 0 ? 'pointer' : 'default';
                    }
                }
            });

            this.charts.set('vehicleStatusChart', chart);
            return chart;
        },

        /**
         * Create Fuel Cost Bar Chart
         * @param {HTMLCanvasElement} canvas - Chart canvas element
         * @param {Array} data - Fuel data array with month/total structure
         */
        createFuelChart(canvas, data) {
            const ctx = canvas.getContext('2d');
            
            // Process data for chart consumption
            const labels = data.map(item => {
                const date = new Date(item.month + '-01');
                return date.toLocaleDateString('it-IT', { 
                    month: 'short', 
                    year: '2-digit' 
                });
            });
            const values = data.map(item => item.total);

            const chart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        data: values,
                        backgroundColor: (ctx) => {
                            const gradient = ctx.chart.ctx.createLinearGradient(0, 0, 0, 400);
                            gradient.addColorStop(0, '#3b82f6');
                            gradient.addColorStop(1, '#1d4ed8');
                            return gradient;
                        },
                        borderRadius: 6,
                        borderSkipped: false,
                    }]
                },
                options: {
                    ...CONFIG.charts,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: 'rgba(0, 0, 0, 0.8)',
                            titleColor: '#ffffff',
                            bodyColor: '#ffffff',
                            borderColor: 'rgba(255, 255, 255, 0.1)',
                            borderWidth: 1,
                            cornerRadius: 8,
                            padding: 12,
                            callbacks: {
                                label: function(context) {
                                    return `Carburante: €${context.parsed.y.toLocaleString('it-IT')}`;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            grid: {
                                color: 'rgba(0, 0, 0, 0.05)',
                                drawBorder: false
                            },
                            ticks: {
                                callback: function(value) {
                                    return '€' + value.toLocaleString('it-IT');
                                }
                            }
                        },
                        x: {
                            grid: {
                                display: false,
                                drawBorder: false
                            }
                        }
                    }
                }
            });

            this.charts.set('fuelChart', chart);
            return chart;
        },

        /**
         * Create Expense Reports Bar Chart
         * @param {HTMLCanvasElement} canvas - Chart canvas element
         * @param {Array} data - Reports data array
         */
        createReportsChart(canvas, data) {
            const ctx = canvas.getContext('2d');
            
            const labels = data.map(item => {
                const date = new Date(item.month + '-01');
                return date.toLocaleDateString('it-IT', { 
                    month: 'short', 
                    year: '2-digit' 
                });
            });
            const values = data.map(item => item.total);

            const chart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        data: values,
                        backgroundColor: (ctx) => {
                            const gradient = ctx.chart.ctx.createLinearGradient(0, 0, 0, 400);
                            gradient.addColorStop(0, '#10b981');
                            gradient.addColorStop(1, '#047857');
                            return gradient;
                        },
                        borderRadius: 6,
                        borderSkipped: false,
                    }]
                },
                options: {
                    ...CONFIG.charts,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: 'rgba(0, 0, 0, 0.8)',
                            titleColor: '#ffffff',
                            bodyColor: '#ffffff',
                            borderColor: 'rgba(255, 255, 255, 0.1)',
                            borderWidth: 1,
                            cornerRadius: 8,
                            padding: 12,
                            callbacks: {
                                label: function(context) {
                                    return `Note Spese: €${context.parsed.y.toLocaleString('it-IT')}`;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            grid: {
                                color: 'rgba(0, 0, 0, 0.05)',
                                drawBorder: false
                            },
                            ticks: {
                                callback: function(value) {
                                    return '€' + value.toLocaleString('it-IT');
                                }
                            }
                        },
                        x: {
                            grid: {
                                display: false,
                                drawBorder: false
                            }
                        }
                    }
                }
            });

            this.charts.set('reportChart', chart);
            return chart;
        },

        /**
         * Cleanup all chart instances
         * Important for preventing memory leaks
         */
        destroyAll() {
            this.charts.forEach(chart => {
                if (chart && typeof chart.destroy === 'function') {
                    chart.destroy();
                }
            });
            this.charts.clear();
        }
    };

    /**
     * Animation Manager Module
     * Handles AOS initialization and custom animations
     */
    const AnimationManager = {
        /**
         * Initialize AOS (Animate On Scroll) library
         * Only if user hasn't disabled animations
         */
        initAOS() {
            if (CONFIG.performance.enableAnimations && typeof AOS !== 'undefined') {
                AOS.init(CONFIG.animations);
            }
        },

        /**
         * Add hover effects to stat cards
         * Uses modern event delegation for performance
         */
        initHoverEffects() {
            if (!CONFIG.performance.enableHover) return;

            const statsGrid = document.getElementById('statsGrid');
            if (!statsGrid) return;

            // Use event delegation for better performance
            statsGrid.addEventListener('mouseenter', (e) => {
                const statCard = e.target.closest('.stat-card');
                if (statCard && !statCard.classList.contains('animating')) {
                    this.addHoverEffect(statCard);
                }
            }, true);

            statsGrid.addEventListener('mouseleave', (e) => {
                const statCard = e.target.closest('.stat-card');
                if (statCard) {
                    this.removeHoverEffect(statCard);
                }
            }, true);
        },

        /**
         * Add smooth hover animation to stat card
         * @param {HTMLElement} card - Stat card element
         */
        addHoverEffect(card) {
            card.style.transform = 'translateY(-4px)';
            card.classList.add('animating');
        },

        /**
         * Remove hover animation from stat card
         * @param {HTMLElement} card - Stat card element
         */
        removeHoverEffect(card) {
            card.style.transform = 'translateY(0)';
            setTimeout(() => {
                card.classList.remove('animating');
            }, 300);
        }
    };

    /**
     * Interaction Manager Module
     * Handles user interactions and accessibility
     */
    const InteractionManager = {
        /**
         * Initialize click handlers for action buttons
         * Adds ripple effects and proper event handling
         */
        initActionButtons() {
            const actionButtons = document.querySelectorAll('.stat-action-btn');
            
            actionButtons.forEach(button => {
                button.addEventListener('click', this.handleActionButtonClick.bind(this));
            });
        },

        /**
         * Handle action button click with ripple effect
         * @param {Event} event - Click event
         */
        handleActionButtonClick(event) {
            event.preventDefault();
            event.stopPropagation();
            
            const button = event.currentTarget;
            
            // Create ripple effect
            if (CONFIG.performance.enableAnimations) {
                this.createRipple(button, event);
            }
            
            // Add visual feedback
            this.addClickFeedback(button);
        },

        /**
         * Create ripple animation effect
         * @param {HTMLElement} element - Target element
         * @param {Event} event - Click event for positioning
         */
        createRipple(element, event) {
            const ripple = document.createElement('span');
            const rect = element.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            
            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = (event.clientX - rect.left - size / 2) + 'px';
            ripple.style.top = (event.clientY - rect.top - size / 2) + 'px';
            ripple.classList.add('ripple');
            
            // Clean up existing ripples
            const existingRipple = element.querySelector('.ripple');
            if (existingRipple) {
                existingRipple.remove();
            }
            
            element.appendChild(ripple);
            
            // Remove ripple after animation
            setTimeout(() => {
                ripple.remove();
            }, 600);
        },

        /**
         * Add click feedback to button
         * @param {HTMLElement} button - Button element
         */
        addClickFeedback(button) {
            button.style.transform = 'scale(0.95)';
            
            setTimeout(() => {
                button.style.transform = 'scale(1)';
            }, 150);
        },

        /**
         * Initialize keyboard navigation support
         * Ensures dashboard is fully accessible
         */
        initKeyboardNavigation() {
            const statCards = document.querySelectorAll('.stat-card');
            
            statCards.forEach(card => {
                card.addEventListener('keydown', (event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        const link = card.querySelector('.stretched-link');
                        if (link) {
                            link.click();
                        }
                    }
                });
            });
        }
    };

    /**
     * Data Manager Module
     * Handles data extraction from Thymeleaf variables
     */
    const DataManager = {
        /**
         * Extract chart data from Thymeleaf inline variables
         * Provides fallback data for development/testing
         */
        getChartData() {
            // Vehicle status data - from Thymeleaf or fallback
            const vehicleStatusLabels = window.vehicleStatusLabels || 
                ['In servizio', 'Assegnato', 'In manutenzione', 'Fuori servizio'];
            const vehicleStatusValues = window.vehicleStatusValues || [2, 2, 1, 0];

            // Fuel cost data - from Thymeleaf or fallback
            const fuelCosts = window.fuelCosts || [
                {month: '2025-01', total: 50},
                {month: '2025-02', total: 75},
                {month: '2025-03', total: 125},
                {month: '2025-04', total: 100},
                {month: '2025-05', total: 150},
                {month: '2025-06', total: 300},
                {month: '2025-07', total: 125}
            ];

            // Report balances data - from Thymeleaf or fallback
            const reportBalances = window.reportBalances || [
                {month: '2025-01', total: 200},
                {month: '2025-02', total: 150},
                {month: '2025-03', total: 300},
                {month: '2025-04', total: 250},
                {month: '2025-05', total: 400},
                {month: '2025-06', total: 350},
                {month: '2025-07', total: 180}
            ];

            return {
                vehicleStatus: {
                    labels: vehicleStatusLabels,
                    values: vehicleStatusValues
                },
                fuelCosts,
                reportBalances
            };
        }
    };

    /**
     * Error Handler Module
     * Centralized error handling with user feedback
     */
    const ErrorHandler = {
        /**
         * Log error and show user-friendly message
         * @param {Error} error - Error object
         * @param {string} context - Context where error occurred
         */
        handleError(error, context) {
            console.error(`Welcome Dashboard Error [${context}]:`, error);
            
            // In production, you might want to send errors to monitoring service
            // this.reportError(error, context);
            
            // Show user-friendly message
            this.showErrorMessage(`Si è verificato un errore nel caricamento del ${context}. Ricarica la pagina.`);
        },

        /**
         * Show error message to user
         * @param {string} message - Error message
         */
        showErrorMessage(message) {
            // Create or update error toast
            const toastContainer = this.getToastContainer();
            const toast = this.createErrorToast(message);
            toastContainer.appendChild(toast);
            
            // Auto-remove after 5 seconds
            setTimeout(() => {
                toast.remove();
            }, 5000);
        },

        /**
         * Get or create toast container
         * @returns {HTMLElement} Toast container element
         */
        getToastContainer() {
            let container = document.getElementById('toast-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'toast-container';
                container.className = 'position-fixed top-0 end-0 p-3';
                container.style.zIndex = '1055';
                document.body.appendChild(container);
            }
            return container;
        },

        /**
         * Create error toast element
         * @param {string} message - Error message
         * @returns {HTMLElement} Toast element
         */
        createErrorToast(message) {
            const toast = document.createElement('div');
            toast.className = 'toast align-items-center text-bg-danger border-0';
            toast.setAttribute('role', 'alert');
            toast.innerHTML = `
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            `;
            
            // Initialize Bootstrap toast
            if (window.bootstrap && bootstrap.Toast) {
                new bootstrap.Toast(toast).show();
            }
            
            return toast;
        }
    };

    /**
     * Main initialization function
     * Orchestrates all modules and handles errors gracefully
     */
    function init() {
        try {
            // Wait for DOM to be fully loaded
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', init);
                return;
            }

            console.log('Initializing Welcome Dashboard...');

            // Initialize Chart.js global settings
            ChartManager.initGlobalDefaults();

            // Get data from backend
            const data = DataManager.getChartData();

            // Initialize charts with error handling
            try {
                const vehicleCanvas = document.getElementById('vehicleStatusChart');
                if (vehicleCanvas) {
                    ChartManager.createVehicleChart(
                        vehicleCanvas, 
                        data.vehicleStatus.labels, 
                        data.vehicleStatus.values
                    );
                }
            } catch (error) {
                ErrorHandler.handleError(error, 'grafico veicoli');
            }

            try {
                const fuelCanvas = document.getElementById('fuelChart');
                if (fuelCanvas) {
                    ChartManager.createFuelChart(fuelCanvas, data.fuelCosts);
                }
            } catch (error) {
                ErrorHandler.handleError(error, 'grafico carburante');
            }

            try {
                const reportCanvas = document.getElementById('reportChart');
                if (reportCanvas) {
                    ChartManager.createReportsChart(reportCanvas, data.reportBalances);
                }
            } catch (error) {
                ErrorHandler.handleError(error, 'grafico note spese');
            }

            // Initialize animations
            AnimationManager.initAOS();
            AnimationManager.initHoverEffects();

            // Initialize interactions
            InteractionManager.initActionButtons();
            InteractionManager.initKeyboardNavigation();

            console.log('Welcome Dashboard initialized successfully');

        } catch (error) {
            ErrorHandler.handleError(error, 'inizializzazione dashboard');
        }
    }

    /**
     * Cleanup function for page unload
     * Prevents memory leaks by destroying chart instances
     */
    function cleanup() {
        console.log('Cleaning up Welcome Dashboard...');
        ChartManager.destroyAll();
    }

    // Listen for page unload to cleanup resources
    window.addEventListener('beforeunload', cleanup);

    // Public API
    return {
        init,
        cleanup,
        // Expose modules for potential external use
        ChartManager,
        AnimationManager,
        InteractionManager,
        DataManager,
        ErrorHandler
    };
})();

// Auto-initialize when script loads
WelcomeDashboard.init();