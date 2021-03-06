package ee.smkv.calc.loan;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import ee.smkv.calc.loan.model.Loan;
import ee.smkv.calc.loan.model.Payment;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ScheduleFragment extends SherlockFragment implements Observer {

    private CreateScheduleTableTask scheduleTableTask;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.schedule, container, false);

        TableLayout table = (TableLayout) view.findViewById(R.id.scheduleTable);
        TableRow tableHeader = (TableRow) table.getChildAt(0);
        int bg = ThemeResolver.isLight(view.getContext()) ? R.drawable.row_header : R.drawable.row_header_dark;
        tableHeader.setBackgroundResource(bg);
        applyBackground(tableHeader, bg);

        setupScheduleTableData(view);
        LoanDispatcher.getInstance().addObserver(this);
        return view;
    }

    private void applyBackground(ViewGroup viewGroup, int bg) {
        Drawable background = getResources().getDrawable(bg);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            viewGroup.getChildAt(i).setBackgroundDrawable(background);
        }
    }

    private void setupScheduleTableData(View view) {
        Loan loan = StartActivity.loan;
        if (loan.isCalculated()) {
            List<Payment> payments = loan.getPayments();
            Payment[] array = new Payment[payments.size()];
            scheduleTableTask = new CreateScheduleTableTask(view.getContext(), (ProgressBar) view.findViewById(R.id.scheduleLoading));
            scheduleTableTask.execute(payments.toArray(array));
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LoanDispatcher.getInstance().deleteObserver(this);
    }

    @Override
    public void onDetach() {
        if (scheduleTableTask != null) {
            scheduleTableTask.cancel(true);
        }
        super.onDetach();
    }

    @Override
    public void update(Observable observable, Object data) {
        getView().findViewById(R.id.scheduleScrollView).setVisibility(View.GONE);
        setupScheduleTableData(getView());
    }

    public class CreateScheduleTableTask extends AsyncTask<Payment, Integer, List<View>> {

        private final Context context;
        private final ProgressBar progressBar;
        private final int background;


        public CreateScheduleTableTask(Context context, ProgressBar progressBar) {
            this.context = context;
            this.progressBar = progressBar;
            background = ThemeResolver.isLight(context) ? R.drawable.row_border : R.drawable.row_border_dark;

        }


        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<View> doInBackground(Payment... payments) {
            this.progressBar.setMax(payments.length);
            List<View> list = new ArrayList<View>(payments.length + 1);

            Loan loan = StartActivity.loan;
            boolean hasAnyCommission = loan.hasAnyCommission();


            if (loan.hasDownPayment() || loan.hasDisposableCommission()) {
                ViewGroup tableRow = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.schedule_row, null);
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentNr), 0);
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentBalance), loan.getAmount());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentPrincipal), loan.getDownPaymentPayment());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentInterest), 0);
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentCommission), loan.getDisposableCommissionPayment());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentAmount), loan.getDownPaymentPayment().add(loan.getDisposableCommissionPayment()));
                if (!hasAnyCommission) {
                    tableRow.findViewById(R.id.schedulePaymentCommission).setVisibility(View.GONE);
                }
                tableRow.setBackgroundResource(background);
                applyBackground(tableRow , background);
                list.add(tableRow);
                this.publishProgress(0);
            }

            for (Payment payment : payments) {
                ViewGroup tableRow = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.schedule_row, null);
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentNr), payment.getNr());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentBalance), payment.getBalance());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentPrincipal), payment.getPrincipal());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentInterest), payment.getInterest());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentCommission), payment.getCommission());
                Utils.setNumber((TextView) tableRow.findViewById(R.id.schedulePaymentAmount), payment.getAmount());
                if (!hasAnyCommission) {
                    tableRow.findViewById(R.id.schedulePaymentCommission).setVisibility(View.GONE);
                }
                tableRow.setBackgroundResource(background);
                applyBackground(tableRow , background);
                list.add(tableRow);
                this.publishProgress(payment.getNr());
            }

            return list;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            this.progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<View> views) {
            TableLayout table = (TableLayout) getView().findViewById(R.id.scheduleTable);

            int childCount = table.getChildCount();
            for (int i = 1; i < childCount; i++) {
                table.removeViewAt(1);
            }

            if (!StartActivity.loan.hasAnyCommission()) {
                getView().findViewById(R.id.paymentCommissionHeader).setVisibility(View.GONE);
            }
            for (View view : views) {
                table.addView(view);
            }
            getView().findViewById(R.id.scheduleScrollView).setVisibility(View.VISIBLE);
            closeDialog();
            scheduleTableTask = null;
        }

        private void closeDialog() {
            try {
                progressBar.setVisibility(View.GONE);
            } catch (Exception ignore) {

            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            closeDialog();
        }
    }

}
