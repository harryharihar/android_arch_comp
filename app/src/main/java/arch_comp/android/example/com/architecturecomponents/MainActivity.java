/*
 * Copyright 2017 Nazmul Idris. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package arch_comp.android.example.com.architecturecomponents;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

enum Tags {
    viewmodel
}

public class MainActivity extends AppCompatActivity {
    private TextView dataTextView;
    private TextView counterTextView;
    private StateViewModel stateViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataTextView = findViewById(R.id.data_textview);
        counterTextView = findViewById(R.id.counter_textview);
        setupModelView();
        attachObservers();
    }

    // Deal with loading state from ViewModel
    private void setupModelView() {
        stateViewModel = ViewModelProviders.of(this).get(StateViewModel.class);
        dataTextView.setText(String.format("Data: %s", stateViewModel.getData()));
    }

    private void attachObservers() {
        stateViewModel
                .getCounter()
                .observe(
                        this,
                        count -> {
                            counterTextView.setText(
                                    String.format("Count: %s", Long.toString(count)));
                        });
    }
}

class StateViewModel extends AndroidViewModel {
    private final ScheduledExecutorService myExecutor;
    private String mData; // This value doesn't change after it is initialized
    private CounterLiveData mCounter = new CounterLiveData(); // This value changes over time

    public StateViewModel(Application context) {
        super(context);
        myExecutor = Executors.newSingleThreadScheduledExecutor();
        myExecutor.scheduleWithFixedDelay(this::recurringTask, 0, 1, TimeUnit.SECONDS);
        Log.d(Tags.viewmodel.name(), "ViewModel constructor: created executor");
    }

    // Counter
    public void recurringTask() {
        long counter = mCounter.get();
        Log.d(Tags.viewmodel.name(), counter % 2 == 0 ? "task: tick" : "task: tock");
        mCounter.set(counter + 1);
    }

    public CounterLiveData getCounter() {
        return mCounter;
    }

    // Data
    public void setData(String mData) {
        this.mData = mData;
    }

    public String getData() {
        if (isDataSet()) {
            Toast.makeText(getApplication(), "Re-using ViewModel", Toast.LENGTH_SHORT).show();
        } else {
            setData(UUID.randomUUID().toString());
            Toast.makeText(getApplication(), "This is a new ViewModel", Toast.LENGTH_SHORT).show();
        }
        return mData;
    }

    public boolean isDataSet() {
        return mData != null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        myExecutor.shutdown();
        Log.d(Tags.viewmodel.name(), mCounter.toString());
        Log.d(Tags.viewmodel.name(), "onCleared: lifecycle of activity finished");
    }
}

class CounterLiveData extends MutableLiveData<Long> {
    public String toString() {
        return String.format("count=%d", getValue());
    }

    public Long get() {
        return getValue() == null ? 0L : getValue();
    }

    public void set(Long value) {
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            // UI thread
            setValue(value);
        } else {
            // Non UI thread
            postValue(value);
        }
    }
}
