/**
 * Settings Page Enhanced JavaScript
 * Handles animations, interactions, and accessibility features
 * for the settings page of Veely Fleet Management System
 */

// Settings page main class for better organization
class SettingsPage {
    constructor() {
        this.cards = [];
        this.searchInput = null;
        this.isInitialized = false;
        
        // Initialize when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }

    /**
     * Initialize the settings page functionality
     */
    init() {
        if (this.isInitialized) return;
        
        this.initializeElements();
        this.initializeAOS();
        this.setupEventListeners();
        this.setupAccessibility();
        this.setupPerformanceOptimizations();
        
        this.isInitialized = true;
        console.log('Settings page initialized successfully');
    }

    /**
     * Cache DOM elements for performance
     */
    initializeElements() {
        this.cards = document.querySelectorAll('.settings-card');
        this.sections = document.querySelectorAll('.settings-section');
        this.links = document.querySelectorAll('.settings-link');
        
        // Create search functionality placeholder for future enhancement
        this.createSearchInterface();
    }

    /**
     * Initialize AOS (Animate On Scroll) library with custom settings
     */
    initializeAOS() {
        if (typeof AOS !== 'undefined') {
            AOS.init({
                duration: 600,
                easing: 'ease-out-cubic',
                once: true, // Animation triggers only once
                offset: 100, // Trigger animation 100px before element enters viewport
                delay: 50, // Global delay
                disable: 'mobile' // Disable on mobile for better performance
            });
            
            console.log('AOS animations initialized');
        }
    }

    /**
     * Setup event listeners for interactive elements
     */
    setupEventListeners() {
        // Add click analytics tracking for settings cards
        this.cards.forEach((card, index) => {
            const link = card.querySelector('.settings-link');
            if (link) {
                // Track card interactions for analytics
                link.addEventListener('click', (e) => {
                    this.trackCardClick(card, link, index);
                });

                // Add ripple effect on click
                link.addEventListener('click', (e) => {
                    this.createRippleEffect(e, card);
                });
            }
        });

        // Keyboard navigation enhancements
        this.setupKeyboardNavigation();
        
        // Handle window resize for responsive adjustments
        window.addEventListener('resize', this.debounce(() => {
            this.handleResize();
        }, 250));
    }

    /**
     * Track clicks on settings cards for analytics
     * @param {HTMLElement} card - The clicked card element
     * @param {HTMLElement} link - The link element
     * @param {number} index - Card index
     */
    trackCardClick(card, link, index) {
        const category = card.getAttribute('data-category') || 'unknown';
        const title = card.querySelector('.card-title')?.textContent || 'unknown';
        const href = link.getAttribute('href');

        // Log interaction (could be sent to analytics service)
        console.log('Settings card clicked:', {
            category,
            title,
            href,
            index,
            timestamp: new Date().toISOString()
        });

        // Could integrate with Google Analytics, Adobe Analytics, etc.
        // Example: gtag('event', 'settings_card_click', { card_title: title });
    }

    /**
     * Create ripple effect on card click for better user feedback
     * @param {Event} event - Click event
     * @param {HTMLElement} card - Card element
     */
    createRippleEffect(event, card) {
        // Only add ripple if user prefers motion
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            return;
        }

        const rect = card.getBoundingClientRect();
        const ripple = document.createElement('span');
        const size = Math.max(rect.width, rect.height);
        const x = event.clientX - rect.left - size / 2;
        const y = event.clientY - rect.top - size / 2;

        ripple.style.cssText = `
            position: absolute;
            width: ${size}px;
            height: ${size}px;
            left: ${x}px;
            top: ${y}px;
            background: rgba(59, 130, 246, 0.3);
            border-radius: 50%;
            transform: scale(0);
            animation: ripple 0.6s linear;
            pointer-events: none;
            z-index: 1;
        `;

        ripple.classList.add('ripple');
        card.style.position = 'relative';
        card.appendChild(ripple);

        // Remove ripple after animation
        setTimeout(() => {
            if (ripple.parentNode) {
                ripple.parentNode.removeChild(ripple);
            }
        }, 600);
    }

    /**
     * Setup keyboard navigation for better accessibility
     */
    setupKeyboardNavigation() {
        let currentIndex = 0;
        const focusableCards = Array.from(this.links);

        // Arrow key navigation
        document.addEventListener('keydown', (e) => {
            if (!focusableCards.length) return;

            switch (e.key) {
                case 'ArrowRight':
                case 'ArrowDown':
                    e.preventDefault();
                    currentIndex = (currentIndex + 1) % focusableCards.length;
                    focusableCards[currentIndex].focus();
                    break;
                    
                case 'ArrowLeft':
                case 'ArrowUp':
                    e.preventDefault();
                    currentIndex = currentIndex === 0 ? focusableCards.length - 1 : currentIndex - 1;
                    focusableCards[currentIndex].focus();
                    break;
                    
                case 'Home':
                    e.preventDefault();
                    currentIndex = 0;
                    focusableCards[currentIndex].focus();
                    break;
                    
                case 'End':
                    e.preventDefault();
                    currentIndex = focusableCards.length - 1;
                    focusableCards[currentIndex].focus();
                    break;
            }
        });

        // Update current index when focusing via mouse/tab
        focusableCards.forEach((link, index) => {
            link.addEventListener('focus', () => {
                currentIndex = index;
            });
        });
    }

    /**
     * Setup accessibility enhancements
     */
    setupAccessibility() {
        // Add live region for dynamic content announcements
        this.createLiveRegion();

        // Enhance focus management
        this.links.forEach(link => {
            link.addEventListener('focus', () => {
                this.announceFocus(link);
            });
        });

        // Add skip links for screen readers
        this.addSkipLinks();
    }

    /**
     * Create ARIA live region for announcements
     */
    createLiveRegion() {
        const liveRegion = document.createElement('div');
        liveRegion.setAttribute('aria-live', 'polite');
        liveRegion.setAttribute('aria-atomic', 'true');
        liveRegion.className = 'sr-only';
        liveRegion.id = 'settings-live-region';
        document.body.appendChild(liveRegion);
    }

    /**
     * Announce focused element to screen readers
     * @param {HTMLElement} link - Focused link element
     */
    announceFocus(link) {
        const card = link.closest('.settings-card');
        const title = card.querySelector('.card-title')?.textContent;
        const description = card.querySelector('.card-description')?.textContent;
        const category = card.getAttribute('data-category');
        
        const announcement = `${title}. ${description}. Category: ${category}`;
        
        const liveRegion = document.getElementById('settings-live-region');
        if (liveRegion) {
            liveRegion.textContent = announcement;
        }
    }

    /**
     * Add skip navigation links for accessibility
     */
    addSkipLinks() {
        const skipNav = document.createElement('nav');
        skipNav.className = 'skip-nav sr-only';
        skipNav.innerHTML = `
            <a href="#hr-section" class="skip-link">Skip to HR Management</a>
            <a href="#fleet-section" class="skip-link">Skip to Fleet Management</a>
            <a href="#admin-section" class="skip-link">Skip to Administration</a>
        `;
        
        document.body.insertBefore(skipNav, document.body.firstChild);
        
        // Add corresponding IDs to sections
        const sections = document.querySelectorAll('.settings-section');
        const sectionIds = ['hr-section', 'fleet-section', 'admin-section'];
        sections.forEach((section, index) => {
            if (sectionIds[index]) {
                section.id = sectionIds[index];
            }
        });
    }

    /**
     * Create search interface for filtering settings (future enhancement)
     */
    createSearchInterface() {
        // Placeholder for future search functionality
        // This could be implemented to filter settings cards by title/description
        console.log('Search interface placeholder created');
    }

    /**
     * Setup performance optimizations
     */
    setupPerformanceOptimizations() {
        // Intersection Observer for lazy loading enhancements
        if ('IntersectionObserver' in window) {
            this.setupIntersectionObserver();
        }

        // Preload critical setting pages on hover
        this.setupLinkPreloading();
    }

    /**
     * Setup Intersection Observer for performance monitoring
     */
    setupIntersectionObserver() {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    // Card is visible, could trigger additional loading or analytics
                    entry.target.classList.add('in-viewport');
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '50px'
        });

        this.cards.forEach(card => {
            observer.observe(card);
        });
    }

    /**
     * Setup link preloading on hover for faster navigation
     */
    setupLinkPreloading() {
        this.links.forEach(link => {
            let timeoutId;
            
            link.addEventListener('mouseenter', () => {
                // Preload page after 100ms hover to avoid unnecessary requests
                timeoutId = setTimeout(() => {
                    this.preloadPage(link.href);
                }, 100);
            });
            
            link.addEventListener('mouseleave', () => {
                clearTimeout(timeoutId);
            });
        });
    }

    /**
     * Preload a page for faster navigation
     * @param {string} href - URL to preload
     */
    preloadPage(href) {
        if (!href || href.includes('#')) return;
        
        // Check if already preloaded
        if (document.querySelector(`link[rel="prefetch"][href="${href}"]`)) {
            return;
        }
        
        const link = document.createElement('link');
        link.rel = 'prefetch';
        link.href = href;
        document.head.appendChild(link);
        
        console.log(`Preloading: ${href}`);
    }

    /**
     * Handle window resize events
     */
    handleResize() {
        // Recalculate any dynamic measurements if needed
        // Refresh AOS on resize if necessary
        if (typeof AOS !== 'undefined') {
            AOS.refresh();
        }
    }

    /**
     * Utility function to debounce events
     * @param {Function} func - Function to debounce
     * @param {number} wait - Wait time in milliseconds
     * @returns {Function} Debounced function
     */
    debounce(func, wait) {
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
     * Public method to refresh the page state
     */
    refresh() {
        if (typeof AOS !== 'undefined') {
            AOS.refresh();
        }
        console.log('Settings page refreshed');
    }

    /**
     * Public method to get current page statistics
     * @returns {Object} Page statistics
     */
    getStats() {
        return {
            totalCards: this.cards.length,
            totalSections: this.sections.length,
            visibleCards: document.querySelectorAll('.settings-card.in-viewport').length,
            isInitialized: this.isInitialized
        };
    }
}

// CSS for skip links and accessibility features
const accessibilityStyles = `
    .sr-only {
        position: absolute !important;
        width: 1px !important;
        height: 1px !important;
        padding: 0 !important;
        margin: -1px !important;
        overflow: hidden !important;
        clip: rect(0, 0, 0, 0) !important;
        white-space: nowrap !important;
        border: 0 !important;
    }
    
    .skip-nav {
        position: absolute;
        top: 0;
        left: 0;
        z-index: 9999;
    }
    
    .skip-link {
        position: absolute;
        top: -40px;
        left: 6px;
        background: var(--primary-600, #2563eb);
        color: white;
        padding: 8px 16px;
        text-decoration: none;
        border-radius: 4px;
        font-weight: 600;
        transition: top 0.3s;
    }
    
    .skip-link:focus {
        top: 6px;
    }
    
    @keyframes ripple {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }
`;

// Inject accessibility styles
const styleSheet = document.createElement('style');
styleSheet.textContent = accessibilityStyles;
document.head.appendChild(styleSheet);

// Initialize the settings page
const settingsPage = new SettingsPage();

// Export for potential external use
window.SettingsPage = SettingsPage;
window.settingsPageInstance = settingsPage;