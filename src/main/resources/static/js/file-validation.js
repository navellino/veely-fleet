// Validazione file lato client
const FileValidator = {
    maxSize: 10 * 1024 * 1024, // 10MB
    maxImageSize: 5 * 1024 * 1024, // 5MB
    
    allowedDocumentTypes: [
        'application/pdf',
        'image/jpeg',
        'image/jpg',
        'image/png',
		'image/webp',
        'application/msword',
		'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
		'application/vnd.ms-outlook'
    ],
    
    allowedImageTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],
    
    validateDocument: function(file) {
        // Controlla dimensione
        if (file.size > this.maxSize) {
            return {
                valid: false,
                message: `Il file supera la dimensione massima di ${this.maxSize / (1024 * 1024)}MB`
            };
        }
        
        // Controlla tipo
        if (!this.allowedDocumentTypes.includes(file.type)) {
            return {
                valid: false,
                message: 'Tipo di file non permesso. Formati accettati: PDF, JPG, PNG, WEBP, DOC, DOCX, MSG'
            };
        }
        
        return { valid: true };
    },
    
    validateImage: function(file) {
        // Controlla dimensione
        if (file.size > this.maxImageSize) {
            return {
                valid: false,
                message: `L'immagine supera la dimensione massima di ${this.maxImageSize / (1024 * 1024)}MB`
            };
        }
        
        // Controlla tipo
        if (!this.allowedImageTypes.includes(file.type)) {
            return {
                valid: false,
                message: 'Formato immagine non permesso. Formati accettati: JPG, PNG, WEBP'
            };
        }
        
        return { valid: true };
    },
    
    // Inizializza validazione su tutti gli input file
    init: function() {
        document.querySelectorAll('input[type="file"]').forEach(input => {
            input.addEventListener('change', function(e) {
                const file = e.target.files[0];
                if (!file) return;
                
                // Determina se è un'immagine basandosi sull'attributo accept
                const isImage = e.target.accept && e.target.accept.includes('image/');
                
                const validation = isImage 
                    ? FileValidator.validateImage(file)
                    : FileValidator.validateDocument(file);
                
                if (!validation.valid) {
                    alert(validation.message);
                    e.target.value = ''; // Reset input
                    return;
                }
                
                // Mostra preview se è un'immagine
                if (isImage) {
                    FileValidator.showImagePreview(file, e.target);
                }
            });
        });
    },
    
    // Mostra anteprima immagine
    showImagePreview: function(file, inputElement) {
        const reader = new FileReader();
        reader.onload = function(e) {
            // Cerca un elemento preview vicino all'input
            let preview = inputElement.parentElement.querySelector('.image-preview');
            if (!preview) {
                preview = document.createElement('img');
                preview.className = 'image-preview mt-2';
                preview.style.maxWidth = '200px';
                preview.style.maxHeight = '200px';
                inputElement.parentElement.appendChild(preview);
            }
            preview.src = e.target.result;
        };
        reader.readAsDataURL(file);
    }
};

// Inizializza quando il DOM è carico
document.addEventListener('DOMContentLoaded', function() {
    FileValidator.init();
});