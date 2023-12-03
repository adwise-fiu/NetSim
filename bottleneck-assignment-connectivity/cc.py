import pandas as pd
from matplotlib import pyplot as plt

import sys

if len(sys.argv) < 2:
    print("Usage: python script.py <filename>")
    sys.exit(1)

filename = sys.argv[1]

# Set the figure size
plt.rcParams["figure.figsize"] = [5.00, 3.50]
plt.rcParams["figure.autolayout"] = True

# Make a list of columns
# columns = ['Time', 'Coverage (Moving)', 'Coverage (Hoovering)']
columns = ['Time', 'Reachability']

# filename = "cc_10_10.csv"
pie = ""
# Read a CSV file
df = pd.read_csv(filename, usecols=columns)

percent_zeros = (df['Reachability'] < 1).mean() * 100
percent_ones = (df['Reachability'] == 1).mean() * 100


fig, ax = plt.subplots(nrows=1, ncols=2, figsize=(10, 5))
# Create a line chart
# plt.figure(1)
ax[0].plot(df['Time'], df['Reachability'])
ax[0].set_xlabel('Time')
ax[0].set_ylabel('Reachability')
ax[0].set_title('Reachability over Time')
# plt.savefig("cc/" + f['png'])
# plt.show()

# Create a pie chart
# plt.figure(2)
labels = ['Not Reachable', 'Reachable']
sizes = [percent_zeros, percent_ones]
ax[1].pie(sizes, labels=labels, autopct='%1.1f%%')
ax[1].set_title('Percentage of Reachability')


# adjust the layout of the subplots to prevent overlapping
plt.tight_layout()

# show both figures
plt.show()

# Plot the lines
# df.plot()


# fig, (ax1, ax2) = plt.subplots(2)
# fig.suptitle('Vertically stacked subplots')
# # ax1.plot(x, y)
# # ax2.plot(x, -y)

# plt.show()
