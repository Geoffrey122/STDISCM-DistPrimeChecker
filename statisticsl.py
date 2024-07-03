import csv
from scipy import stats

# Load the results from the CSV files
def load_results(filename):
    thread_counts = []
    mean_runtimes = []
    with open(filename, 'r') as file:
        reader = csv.reader(file)
        next(reader)  # Skip the header
        for row in reader:
            thread_counts.append(int(row[0]))
            mean_runtimes.append(float(row[1]))
    return thread_counts, mean_runtimes

# Load results from both experiments
_, mean_runtimes_noslaves = load_results('results_no_slaves.csv')
_, mean_runtimes_slaves = load_results('distributed_results.csv')

# Perform the paired t-test
t_statistic, p_value = stats.ttest_rel(mean_runtimes_noslaves, mean_runtimes_slaves)

# Output the results
print(f"Paired t-test results:")
print(f"t-statistic: {t_statistic}")
print(f"p-value: {p_value}")

# Determine if we reject the null hypothesis
alpha = 0.05
if p_value < alpha:
    print("Reject the null hypothesis: There is a significant difference in mean runtimes.")
else:
    print("Fail to reject the null hypothesis: There is no significant difference in mean runtimes.")
