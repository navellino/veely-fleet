package com.veely.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import com.veely.model.ExpenseStatus;
import com.veely.model.PaymentMethod;
import jakarta.validation.constraints.PastOrPresent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expense_report")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseReport {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_report_id")
	 private Long id; //Chiave primaria univoca per la nota spese.
	
	private String expenseReportNum; //Numero di riferimento univoco e leggibile per la nota spese, che segue le policy di numerazione aziendali.
	private long parentExpenseReportID; //Identificatore di una nota spese padre, utile per raggruppare note spese correlate.
	private long orgID; //Identificatore dell'unità organizzativa (business unit) a cui la nota spese è associata.
	
	@ManyToOne(optional = false)
    private Employee employee; //Identificatore della persona le cui spese sono incluse nella nota spese (il richiedente).
	
	private String puorpose;  //Descrizione testuale dello scopo della nota spese (es. "Viaggio cliente a Roma", "Fiera di settore").
	
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "La data di creazione non può essere futura")
    private LocalDate creationDate; //data della nota spese;
	
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate reportSubmitDate; //Data in cui la nota spese viene sottomessa per l'approvazione.
	
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate; //Data di inizio del periodo coperto dalla nota spese.
	
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate; //Data di fine del periodo coperto dalla nota spese.
	
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate finalApprovalDate; //Data in cui si ottiene l'approvazione finale.
	
	private BigDecimal expenseReportTotal; //Importo totale della nota spese nella valuta di rimborso.

    private BigDecimal reimbursableTotal; //Totale degli importi rimborsabili al dipendente.

    private BigDecimal nonReimbursableTotal; //Totale degli importi non rimborsabili (es. spese pagate con carta aziendale).

    @ManyToOne
    private Project project; // Commessa associata alla nota spese

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethodCode; //Metodo di pagamento per il rimborso (es. Bonifico, Busta Paga).
	
	@Enumerated(EnumType.STRING)
	private ExpenseStatus expenseStatus; //Stato corrente del workflow (es. Draft, Submitted, Approved, Rejected, Paid).
	
}
